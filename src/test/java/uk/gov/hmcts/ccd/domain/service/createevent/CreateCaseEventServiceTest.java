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
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.*;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CreateCaseEventServiceTest {

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

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.00Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateCaseEventService createEventService;

    private Event event;

    private Map<String, JsonNode> data;
    private CaseType caseType;
    private CaseEvent eventTrigger;
    private CaseDetails caseDetails;
    private CaseDetails caseDetailsBefore;
    private CaseState postState;
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
        caseDataContent = newCaseDataContent().withEvent(event).withData(data).withToken(TOKEN).withIgnoreWarning(IGNORE_WARNING).build();
        final Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setId(JURISDICTION_ID);
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);
        caseType = new CaseType();
        caseType.setId(CASE_TYPE_ID);
        caseType.setJurisdiction(jurisdiction);
        caseType.setVersion(version);
        eventTrigger = new CaseEvent();
        eventTrigger.setPostState(POST_STATE);
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
        postState = new CaseState();
        postState.setId(POST_STATE);
        IdamUser user = new IdamUser();
        user.setId("123");

        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(JURISDICTION_ID, caseType);
        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, EVENT_ID);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, eventTrigger);
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);
        doReturn(postState).when(caseTypeService).findState(caseType, POST_STATE);
        doReturn(user).when(userRepository).getUser();
        doReturn(caseDetailsBefore).when(caseService).clone(caseDetails);
        doReturn(data).when(fieldProcessorService).processData(any(), any(), any(CaseEvent.class));
        given(callbackInvoker.invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            any())).willReturn(aboutToSubmitCallbackResponse);
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
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("should not update Last state modified")
    void shouldNotUpdateLastStateModifiedWhenStateTransitionNotOccurred() {
        caseDetailsBefore.setLastStateModifiedDate(LAST_MODIFIED);
        caseDetailsBefore.setState(PRE_STATE_ID);
        eventTrigger = new CaseEvent();
        eventTrigger.setPostState(PRE_STATE_ID);

        CaseState state = new CaseState();
        state.setId(PRE_STATE_ID);

        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, EVENT_ID);
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, eventTrigger);
        doReturn(state).when(caseTypeService).findState(caseType, PRE_STATE_ID);

        CreateCaseEventResult caseEventResult = createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertThat(caseEventResult.getSavedCaseDetails().getState()).isEqualTo(PRE_STATE_ID);
        assertThat(caseEventResult.getSavedCaseDetails().getLastStateModifiedDate()).isEqualTo(LAST_MODIFIED);
    }

    @Test
    @DisplayName("should invoke about to submit callback")
    void shouldInvokeAboutToSubmitCallback() {
        createCaseEvent();

        verify(callbackInvoker).invokeAboutToSubmitCallback(eventTrigger,
            caseDetailsBefore,
            caseDetails,
            caseType,
            IGNORE_WARNING);
    }

    private void createCaseEvent() {
        createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

}
