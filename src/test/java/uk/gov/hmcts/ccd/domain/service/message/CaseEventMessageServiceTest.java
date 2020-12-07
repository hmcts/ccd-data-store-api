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
import uk.gov.hmcts.ccd.data.message.MessageCandidateRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class CaseEventMessageServiceTest {

    private static final String USER_ID = "123";
    private static final Long CASE_ID = 456L;
    private static final String EVENT_ID = "SomeEvent";
    private static final String EVENT_NAME = "Some event";
    private static final String CASE_EVENT = "CASE_EVENT";
    private static final String JURISDICTION = "Some Jurisdiction";
    private static final String CASE_TYPE = "Some case type";
    private static final String STATE = "State one";
    private static final LocalDateTime DATE_TIME =
        LocalDateTime.of(2000, 12, 07, 13, 13, 13);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageCandidateRepository messageCandidateRepository;

    @InjectMocks
    private CaseEventMessageService caseEventMessageService;
    private Event event;
    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(getUser()).when(userRepository).getUser();

        caseEventDefinition = buildEventTrigger();
        event = buildEvent();
        caseDetails = buildCaseDetails();
    }

    @Test
    @DisplayName("should persist case event message")
    void shouldPersistEvent() {
        final ArgumentCaptor<MessageQueueCandidate> messageCaptor =
            ArgumentCaptor.forClass(MessageQueueCandidate.class);

        MessageInformation messageInformation = buildMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        caseEventMessageService.handleMessage(event,
            caseEventDefinition,
            caseDetails);

        assertAll(
            () -> verify(messageCandidateRepository).set(messageCaptor.capture()),
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
        msgInfo.setEventInstanceId(event.getEventId());
        msgInfo.setEventTimestamp(caseDetails.getLastStateModifiedDate());
        msgInfo.setEventId(event.getEventId());
        msgInfo.setUserId(userRepository.getUser().getId());
        msgInfo.setPreviousStateId(caseEventDefinition.getPreStates().get(0));
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
        caseDetails.setLastStateModifiedDate(DATE_TIME);
        return caseDetails;
    }

    private Event buildEvent() {
        final Event event = new Event();
        event.setEventId(EVENT_ID);
        return event;
    }

    private IdamUser getUser() {
        final IdamUser user = new IdamUser();
        user.setEmail("test@email.com");
        user.setId(USER_ID);
        return user;
    }

}
