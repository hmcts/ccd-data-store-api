package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.message.CaseEventMessageService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CreateCaseEventDefinitionServiceTest {

    private static final String USER_ID = "123";
    private static final String JURISDICTION_ID = "SSCS";
    private static final String CASE_TYPE_ID = "Claim";
    private static final String CASE_REFERENCE = "1234123412341236";
    private static final String TOKEN = "eygeyvcey12w2";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String EVENT_ID = "UpdateCase";
    private static final String PRE_STATE_ID = "Created";
    private static final String POST_STATE = "Updated";
    private static final Integer VERSION_NUMBER = 1;
    private static final LocalDateTime LAST_MODIFIED = LocalDateTime.of(2015, 12, 21, 15, 30);

    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseAuditEventRepository caseAuditEventRepository;
    @Mock
    private EventTriggerService eventTriggerService;
    @Mock
    private EventTokenService eventTokenService;
    @Mock
    private CaseDataService caseDataService;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private CaseSanitiser caseSanitiser;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private UIDService uidService;
    @Mock
    private SecurityClassificationService securityClassificationService;
    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;
    @Mock
    private CaseService caseService;
    @Mock
    private UserAuthorisation userAuthorisation;
    @Mock
    private FieldProcessorService fieldProcessorService;
    @Mock
    private Clock clock;
    @Mock
    private CasePostStateService casePostStateService;
    @Mock
    private CaseEventMessageService caseEventMessageService;

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.00Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateCaseEventService createEventService;

    private Event event;

    private Map<String, JsonNode> data;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;
    private CaseDetails caseDetailsBefore;
    private CaseStateDefinition postState;
    private CaseDataContent caseDataContent;

    private static Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setSummary("Update case summary");
        event.setDescription("Update case description");
        return event;
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        event = buildEvent();
        data = buildJsonNodeData();
        caseDataContent = newCaseDataContent().withEvent(event).withData(data).withToken(TOKEN)
                .withIgnoreWarning(IGNORE_WARNING).build();
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(JURISDICTION_ID);
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);
        caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        caseTypeDefinition.setVersion(version);
        caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setPostStates(getEventPostStates(POST_STATE));
        caseEventDefinition.setPublish(Boolean.TRUE);
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setUrl("http://www.yahoo.com");
        significantItem.setDescription("description");
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();
        aboutToSubmitCallbackResponse.setSignificantItem(significantItem);
        aboutToSubmitCallbackResponse.setState(Optional.empty());
        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState(PRE_STATE_ID);
        caseDetails.setLastModified(LAST_MODIFIED);
        caseDetails.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore = caseDetails.shallowClone();
        postState = new CaseStateDefinition();
        postState.setId(POST_STATE);
        IdamUser user = new IdamUser();
        user.setId("123");

        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(JURISDICTION_ID, caseTypeDefinition);
        doReturn(caseEventDefinition).when(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_ID);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, caseEventDefinition);
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);
        doReturn(postState).when(caseTypeService).findState(caseTypeDefinition, POST_STATE);
        doReturn(user).when(userRepository).getUser();
        doReturn(caseDetailsBefore).when(caseService).clone(caseDetails);
        doReturn(data).when(fieldProcessorService).processData(any(), any(), any(CaseEventDefinition.class));
        given(callbackInvoker.invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            any())).willReturn(aboutToSubmitCallbackResponse);
        doReturn(POST_STATE).when(this.casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));
    }

    private List<EventPostStateDefinition> getEventPostStates(String... postStateReferences) {
        List<EventPostStateDefinition> postStates = new ArrayList<>();
        int i = 0;
        for (String reference : postStateReferences) {
            EventPostStateDefinition definition = new EventPostStateDefinition();
            definition.setPostStateReference(reference);
            definition.setPriority(++i);
            postStates.add(definition);
        }
        return postStates;
    }

    @Test
    @DisplayName("should create copy of case before mutating it")
    void shouldCreateCopyOfCaseBeforeMutation() {
        createCaseEvent();

        verify(caseService).clone(caseDetails);
    }

    @Test
    @DisplayName("should update Last state modified")
    void shouldUpdateLastStateModifiedWhenStateTransitionOccurred() {
        caseDetailsBefore.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore.setState(PRE_STATE_ID);

        CreateCaseEventResult caseEventResult = createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(POST_STATE);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate())
                .isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("should not update Last state modified")
    void shouldNotUpdateLastStateModifiedWhenStateTransitionNotOccurred() {
        caseDetailsBefore.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore.setState(PRE_STATE_ID);
        caseEventDefinition = new CaseEventDefinition();
        doReturn(PRE_STATE_ID).when(this.casePostStateService)
            .evaluateCaseState(any(CaseEventDefinition.class), any(CaseDetails.class));

        CaseStateDefinition state = new CaseStateDefinition();
        state.setId(PRE_STATE_ID);

        doReturn(caseEventDefinition).when(eventTriggerService).findCaseEvent(caseTypeDefinition, EVENT_ID);
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, caseEventDefinition);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, PRE_STATE_ID);

        CreateCaseEventResult caseEventResult = createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(PRE_STATE_ID);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate()).isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("should invoke about to submit callback")
    void shouldInvokeAboutToSubmitCallback() {
        createCaseEvent();

        verify(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
            caseDetailsBefore,
            caseDetails,
            caseTypeDefinition,
            IGNORE_WARNING);
    }

    private void createCaseEvent() {
        createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

}
