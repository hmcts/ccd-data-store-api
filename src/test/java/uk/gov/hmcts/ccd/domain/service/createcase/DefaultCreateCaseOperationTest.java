package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.EventBuilder;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyZeroInteractions;

class DefaultCreateCaseOperationTest {

    @Mock
    private CaseState caseEventState;
    @Mock
    private CaseDetails savedCaseType;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private EventTriggerService eventTriggerService;
    @Mock
    private EventTokenService eventTokenService;
    @Mock
    private CaseDataService caseDataService;
    @Mock
    private SubmitCaseTransaction submitCaseTransaction;
    @Mock
    private CaseSanitiser caseSanitiser;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;
    @Mock
    private ResponseEntity<AfterSubmitCallbackResponse> response;
    @Mock
    private AfterSubmitCallbackResponse responseBody;

    private DefaultCreateCaseOperation defaultCreateCaseOperation;

    private static final String UID = "244";
    private static final String JURISDICTION_ID = "jid";
    private static final String CASE_TYPE_ID = "cti";
    private Event event = buildEvent();

    private static Map<String, JsonNode> data;

    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String TOKEN = "toke";

    private static final IDAMProperties IDAM_PROPERTIES = buildIDAMUser();
    private static final CaseType CASE_TYPE = buildCaseType();
    private CaseEvent eventTrigger;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        defaultCreateCaseOperation = new DefaultCreateCaseOperation(userRepository,
                                                                    caseDefinitionRepository,
                                                                    eventTriggerService,
                                                                    eventTokenService,
                                                                    caseDataService,
                                                                    submitCaseTransaction,
                                                                    caseSanitiser,
                                                                    caseTypeService,
                                                                    callbackInvoker,
                                                                    validateCaseFieldsOperation);
        data = buildJsonNodeData();
        given(userRepository.getUserDetails()).willReturn(IDAM_PROPERTIES);
        eventTrigger = buildEventTrigger();
        event = buildEvent();
    }

    @Test
    @DisplayName("Should throws ValidationException when event is null")
    void shouldThrowValidationException_whenEventIsNull() {
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        null,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "Cannot create case because of event is not specified");
    }

    @Test
    @DisplayName("Should throws ValidationException when event id is null")
    void shouldThrowValidationException_whenEventIdIsNull() {
        event.setEventId(null);
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        event,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "Cannot create case because of event is not specified");
    }

    @Test
    @DisplayName("Should throws ValidationException when case type is not found")
    void shouldThrowValidationException_whenCaseTypeIsNotFound() {
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        event,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "Cannot find case type definition for cti");
    }

    @Test
    @DisplayName("Should throws ValidationException when case type is not defined for Jurisdiction")
    void shouldThrowValidationException_whenCaseTypeIsNotDefinedForJurisdiction() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        event,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "Cannot create case because of cti is not defined as case type for jid");
    }

    @Test
    @DisplayName("Should throws ValidationException when event id is not known for case type")
    void shouldThrowValidationException_whenEventIdIsUnknown() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        event,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "eid is not a known event ID for the specified case type cti");
    }

    @Test
    @DisplayName("Should throws ValidationException when event pre-state is invalid")
    void shouldThrowValidationException_whenPreStateIsInvalid() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class,
                     () -> defaultCreateCaseOperation.createCaseDetails(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        event,
                                                                        data,
                                                                        IGNORE_WARNING,
                                                                        TOKEN),
                     "Cannot create case because of eventId has pre-states defined");
    }

    @Test
    @DisplayName("Should return saved case details when Submitted Callback url is blank")
    void shouldReturnSavedCaseDetails_whenSubmittedCallBackUrlIsBlank() {
        final String caseEventStateId = "Some state";
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
                                               same(CASE_TYPE),
                                               same(IDAM_PROPERTIES),
                                               same(eventTrigger),
                                               any(CaseDetails.class),
                                               same(IGNORE_WARNING)))
            .willReturn(savedCaseType);
        eventTrigger.setCallBackURLSubmittedEvent("   ");

        final CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails(UID,
                                                                                     JURISDICTION_ID,
                                                                                     CASE_TYPE_ID,
                                                                                     event,
                                                                                     data,
                                                                                     IGNORE_WARNING,
                                                                                     TOKEN);

        final InOrder order = inOrder(eventTokenService,
                                      caseTypeService,
                                      validateCaseFieldsOperation,
                                      submitCaseTransaction);
        final ArgumentCaptor<CaseDetails> caseDetailsArgumentCaptor = ArgumentCaptor.forClass(CaseDetails.class);


        assertAll(() -> assertThat(caseDetails, IsInstanceOf.instanceOf(CaseDetails.class)),
                  () -> order.verify(eventTokenService)
                      .validateToken(TOKEN, UID, eventTrigger, CASE_TYPE.getJurisdiction(), CASE_TYPE),
                  () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data),
                  () -> order.verify(submitCaseTransaction).submitCase(same(event),
                                                                       same(CASE_TYPE),
                                                                       same(IDAM_PROPERTIES),
                                                                       same(eventTrigger),
                                                                       caseDetailsArgumentCaptor.capture(),
                                                                       same(IGNORE_WARNING)),
                  () -> verifyZeroInteractions(callbackInvoker),
                  () -> assertCaseDetails(caseDetailsArgumentCaptor.getValue()),
                  () -> assertThat(caseDetails, is(savedCaseType)));
    }

    @Test
    @DisplayName("Should return saved case details when call back fails")
    void shouldReturnSavedCaseDetails_whenCallBackFails() {
        final String caseEventStateId = "Some state";
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        eventTrigger.setCallBackURLSubmittedEvent("http://localhost/submittedcallback");
        given(callbackInvoker.invokeSubmittedCallback(eventTrigger, null, savedCaseType)).willThrow(new CallbackException(
            "call back exception"));
        given(validateCaseFieldsOperation.validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
                                               same(CASE_TYPE),
                                               same(IDAM_PROPERTIES),
                                               same(eventTrigger),
                                               any(CaseDetails.class),
                                               same(IGNORE_WARNING)))
            .willReturn(savedCaseType);

        defaultCreateCaseOperation.createCaseDetails(UID,
                                                     JURISDICTION_ID,
                                                     CASE_TYPE_ID,
                                                     event,
                                                     data,
                                                     IGNORE_WARNING,
                                                     TOKEN);

        final InOrder order = inOrder(eventTokenService,
                                      caseTypeService,
                                      validateCaseFieldsOperation,
                                      submitCaseTransaction,
                                      callbackInvoker,
                                      savedCaseType);

        assertAll("case details saved when call back fails",
                  () -> order.verify(eventTokenService)
                      .validateToken(TOKEN, UID, eventTrigger, CASE_TYPE.getJurisdiction(), CASE_TYPE),
                  () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data),
                  () -> order.verify(submitCaseTransaction).submitCase(same(event),
                                                                       same(CASE_TYPE),
                                                                       same(IDAM_PROPERTIES),
                                                                       same(eventTrigger),
                                                                       any(CaseDetails.class),
                                                                       same(IGNORE_WARNING)),
                  () -> order.verify(callbackInvoker)
                             .invokeSubmittedCallback(eq(eventTrigger), isNull(CaseDetails.class), same(savedCaseType)),
                  () -> order.verify(savedCaseType).setIncompleteCallbackResponse());
    }

    @Test
    @DisplayName("Should return also call back response when call back is invoked successfully")
    void shouldReturnAlsoCallbackResponse_whenCallBackIsInvokedSuccessfully() {
        final String caseEventStateId = "Some state";
        final String mockCaseTypeId = "mock case type id";
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        eventTrigger.setCallBackURLSubmittedEvent("http://localhost/submittedcallback");
        given(callbackInvoker.invokeSubmittedCallback(eventTrigger,
                                                      null,
                                                      savedCaseType)).willReturn(response);
        given(response.hasBody()).willReturn(true);
        given(response.getBody()).willReturn(responseBody);
        given(response.getStatusCodeValue()).willReturn(200);
        given(savedCaseType.getCaseTypeId()).willReturn(mockCaseTypeId);
        given(validateCaseFieldsOperation.validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
                                               same(CASE_TYPE),
                                               same(IDAM_PROPERTIES),
                                               same(eventTrigger),
                                               any(CaseDetails.class),
                                               same(IGNORE_WARNING)))
            .willReturn(savedCaseType);

        final CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails(UID,
                                                                                     JURISDICTION_ID,
                                                                                     CASE_TYPE_ID,
                                                                                     event,
                                                                                     data,
                                                                                     IGNORE_WARNING,
                                                                                     TOKEN);

        final InOrder order = inOrder(eventTokenService, caseTypeService, validateCaseFieldsOperation, submitCaseTransaction, callbackInvoker, savedCaseType);

        assertAll("Call back response returned successfully",
                  () -> assertThat(caseDetails.getCaseTypeId(), is(mockCaseTypeId)),
                  () -> order.verify(eventTokenService)
                      .validateToken(TOKEN, UID, eventTrigger, CASE_TYPE.getJurisdiction(), CASE_TYPE),
                  () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(JURISDICTION_ID, CASE_TYPE_ID, event, data),
                  () -> order.verify(submitCaseTransaction).submitCase(same(event),
                                                                       same(CASE_TYPE),
                                                                       same(IDAM_PROPERTIES),
                                                                       same(eventTrigger),
                                                                       any(CaseDetails.class),
                                                                       same(IGNORE_WARNING)),
                  () -> order.verify(callbackInvoker)
                             .invokeSubmittedCallback(eq(eventTrigger),
                                                      isNull(CaseDetails.class), same(savedCaseType)),
                  () -> order.verify(savedCaseType).setAfterSubmitCallbackResponseEntity(response));
    }

    private void assertCaseDetails(final CaseDetails details) {
        assertThat(details.getCaseTypeId(), is("cti"));
        assertThat(details.getJurisdiction(), is(JURISDICTION_ID));
    }

    private static Event buildEvent() {
        final Event event = new EventBuilder().build();
        event.setEventId("eid");
        event.setDescription("e-desc");
        event.setSummary("e-summ");
        return event;
    }

    private Map<String, JsonNode> buildJsonNodeData() throws IOException {
        final JsonNode node = new ObjectMapper().readTree("{\n" + "  \"PersonFirstName\": \"ccd-First Name\",\n" + "  \"PersonLastName\": \"Last Name\",\n" + "  " + "\"PersonAddress\": {\n" + "    \"AddressLine1\": \"Address Line 1\",\n" + "    \"AddressLine2\": " + "\"Address Line 2\"\n" + "  }\n" + "}\n");
        final Map<String, JsonNode> map = new HashMap<>();
        map.put("xyz", node);
        return map;
    }

    private static IDAMProperties buildIDAMUser() {
        final IDAMProperties properties = new IDAMProperties();
        properties.setId("pid");
        properties.setRoles(new String[]{"role-A", "role-B"});
        properties.setEmail("ngitb@hmcts.net");
        properties.setForename("Wo");
        properties.setSurname("Mata");
        return properties;
    }

    private static CaseType buildCaseType() {
        final Jurisdiction j = buildJurisdiction();
        final Version version = new Version();
        version.setNumber(67);
        final CaseType caseType = new CaseType();
        caseType.setId("caseTypeId");
        caseType.setName("case type name");
        caseType.setJurisdiction(j);
        caseType.setVersion(version);
        return caseType;
    }

    private static Jurisdiction buildJurisdiction() {
        final Jurisdiction j = new Jurisdiction();
        j.setId(JURISDICTION_ID);
        return j;
    }

    private static CaseEvent buildEventTrigger() {
        final CaseEvent event = new CaseEvent();
        event.setId("eventId");
        event.setName("event Name");
        return event;
    }
}
