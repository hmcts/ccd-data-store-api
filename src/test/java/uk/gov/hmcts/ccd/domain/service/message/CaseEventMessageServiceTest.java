package uk.gov.hmcts.ccd.domain.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.message.MessageCandidateRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DataBlockGenerator;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlockGenerator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class CaseEventMessageServiceTest {

    private static final String USER_ID = "123";
    private static final Long EVENT_INSTANCE_ID = 2L;
    private static final Long CASE_ID = 456L;
    private static final String EVENT_ID = "SomeEvent";
    private static final String EVENT_NAME = "Some event";
    private static final String CASE_EVENT = "CASE_EVENT";
    private static final String JURISDICTION = "Some Jurisdiction";
    private static final String CASE_TYPE = "Some case type";
    private static final String STATE = "State one";
    private static final LocalDateTime DATE_TIME =
        LocalDateTime.of(2000, 12, 07, 13, 13, 13);

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;
    private Clock fixedClock;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private CaseAuditEventRepository caseAuditEventRepository;

    @Mock
    private MessageCandidateRepository messageCandidateRepository;

    @Mock
    private DefinitionBlockGenerator definitionBlockGenerator;

    @Mock
    private DataBlockGenerator dataBlockGenerator;

    @InjectMocks
    private CaseEventMessageService caseEventMessageService;
    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(getUser()).when(userRepository).getUser();
        doReturn(getAuditEvent()).when(caseAuditEventRepository).findByCase(any());
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        caseEventDefinition = buildEventTrigger();
        caseDetails = buildCaseDetails();
    }

    @Test
    @DisplayName("should persist case event message")
    void shouldPersistEvent() {
        final ArgumentCaptor<MessageQueueCandidate> messageCaptor =
            ArgumentCaptor.forClass(MessageQueueCandidate.class);

        MessageInformation messageInformation = buildMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        caseEventMessageService.handleMessage(MessageContext.builder().caseDetails(caseDetails)
                .caseEventDefinition(caseEventDefinition)
                .oldState(STATE).build());

        assertAll(
            () -> verify(messageCandidateRepository).save(messageCaptor.capture()),
            () -> assertMessage(messageCaptor.getValue(), node)
        );
    }

    private void assertMessage(final MessageQueueCandidate messageQueueCandidate, JsonNode node) {
        assertAll(
            () -> assertThat(messageQueueCandidate.getMessageInformation(), is(node)),
            () -> assertThat(messageQueueCandidate.getMessageType(), is(CASE_EVENT)),
            () -> assertNull(messageQueueCandidate.getPublished())
        );
    }

    private MessageInformation buildMessageInformation() {
        final MessageInformation msgInfo = new MessageInformation();
        msgInfo.setCaseId(caseDetails.getReference().toString());
        msgInfo.setJurisdictionId(caseDetails.getJurisdiction());
        msgInfo.setCaseTypeId(caseDetails.getCaseTypeId());
        msgInfo.setEventInstanceId(EVENT_INSTANCE_ID);
        msgInfo.setEventTimestamp(caseDetails.getLastModified());
        msgInfo.setEventId(caseEventDefinition.getId());
        msgInfo.setUserId(userRepository.getUser().getId());
        msgInfo.setPreviousStateId(STATE);
        msgInfo.setNewStateId(caseDetails.getState());
        return msgInfo;
    }


    private CaseEventDefinition buildEventTrigger() {
        final CaseEventDefinition event = new CaseEventDefinition();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        event.setPublish(Boolean.TRUE);
        event.setPreStates(Arrays.asList(STATE));
        return event;
    }

    private CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_ID);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setState(STATE);
        caseDetails.setLastModified(DATE_TIME);
        return caseDetails;
    }

    private IdamUser getUser() {
        final IdamUser user = new IdamUser();
        user.setEmail("test@email.com");
        user.setId(USER_ID);
        return user;
    }

    private List<AuditEvent> getAuditEvent() {
        final AuditEvent event = new AuditEvent();
        event.setId(EVENT_INSTANCE_ID);
        return Arrays.asList(event);
    }

}
