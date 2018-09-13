package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.*;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class DefaultCreateEventOperationTest {

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
    private static final String CALLBACK_URL = "http://sscs.reform.hmcts.net/callback";

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
    private EventValidator eventValidator;
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

    @InjectMocks
    private DefaultCreateEventOperation createEventOperation;

    private Event event;

    private Map<String, JsonNode> data;
    private Jurisdiction jurisdiction;
    private CaseType caseType;
    private CaseEvent eventTrigger;
    private CaseDetails caseDetails;
    private CaseDetails caseDetailsBefore;
    private CaseState postState;
    private IDAMProperties user;

    private static Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setSummary("Update case summary");
        event.setDescription("Update case description");
        return event;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        event = buildEvent();
        data = buildJsonNodeData();

        jurisdiction = new Jurisdiction();
        jurisdiction.setId(JURISDICTION_ID);
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);
        caseType = new CaseType();
        caseType.setId(CASE_TYPE_ID);
        caseType.setJurisdiction(jurisdiction);
        caseType.setVersion(version);
        eventTrigger = new CaseEvent();
        eventTrigger.setPostState(POST_STATE);
        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState(PRE_STATE_ID);
        caseDetails.setLastModified(LAST_MODIFIED);
        caseDetailsBefore = mock(CaseDetails.class);
        postState = new CaseState();
        postState.setId(POST_STATE);
        user = new IDAMProperties();
        user.setId("123");

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(JURISDICTION_ID, caseType);
        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, EVENT_ID);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(caseDetails).when(caseDetailsRepository).lockCase(Long.valueOf(CASE_REFERENCE));
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, eventTrigger);
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);
        doReturn(postState).when(caseTypeService).findState(caseType, POST_STATE);
        doReturn(user).when(userRepository).getUserDetails();
        doReturn(caseDetailsBefore).when(caseService).clone(caseDetails);
        given(callbackInvoker.invokeAboutToSubmitCallback(any(),
                                                          any(),
                                                          any(),
                                                          any(),
                                                          any())).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("should create copy of case before mutating it")
    void shouldCreateCopyOfCaseBeforeMutation() {
        createCaseEvent();

        verify(caseService).clone(caseDetails);
    }

    @Test
    @DisplayName("should not interact with before case details copy")
    void shouldNotInteractWithBeforeCaseDetails() {
        createCaseEvent();

        verifyZeroInteractions(caseDetailsBefore);
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

    @Test
    @DisplayName("should invoke after submit callback")
    void shouldInvokeAfterSubmitCallback() {
        eventTrigger.setCallBackURLSubmittedEvent(CALLBACK_URL);
        AfterSubmitCallbackResponse response = new AfterSubmitCallbackResponse();
        response.setConfirmationHeader("Header");
        response.setConfirmationBody("Body");
        doReturn(ResponseEntity.ok(response)).when(callbackInvoker)
            .invokeSubmittedCallback(eventTrigger,
                caseDetailsBefore,
                caseDetails);

        final CaseDetails caseDetails = createCaseEvent();

        assertAll(
            () -> verify(callbackInvoker).invokeSubmittedCallback(eventTrigger, caseDetailsBefore, this.caseDetails),
            () -> assertThat(caseDetails.getAfterSubmitCallbackResponse().getConfirmationHeader(), is("Header")),
            () -> assertThat(caseDetails.getAfterSubmitCallbackResponse().getConfirmationBody(), is("Body")),
            () -> assertThat(caseDetails.getCallbackResponseStatusCode(), is(SC_OK)),
            () -> assertThat(caseDetails.getCallbackResponseStatus(), is("CALLBACK_COMPLETED"))
        );
    }

    @Test
    @DisplayName("should return incomplete response status if remote endpoint is down")
    void shouldReturnIncomplete() {
        eventTrigger.setCallBackURLSubmittedEvent(CALLBACK_URL);
        doThrow(new CallbackException("Testing failure")).when(callbackInvoker)
            .invokeSubmittedCallback(eventTrigger,
                caseDetailsBefore,
                caseDetails);

        final CaseDetails caseDetails = createCaseEvent();

        assertAll(
            () -> assertNull(caseDetails.getAfterSubmitCallbackResponse()),
            () -> assertThat(caseDetails.getCallbackResponseStatusCode(), is(SC_OK)),
            () -> assertThat(caseDetails.getCallbackResponseStatus(), is("INCOMPLETE_CALLBACK"))
        );
    }

    private CaseDetails createCaseEvent() {
        return createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            event,
            data,
            TOKEN,
            IGNORE_WARNING);
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

}
