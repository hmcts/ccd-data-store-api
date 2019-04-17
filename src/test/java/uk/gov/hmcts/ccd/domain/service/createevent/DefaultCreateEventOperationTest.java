package uk.gov.hmcts.ccd.domain.service.createevent;

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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultCreateEventOperationTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String USER_ID = "123";
    private static final String JURISDICTION_ID = "SSCS";
    private static final String CASE_TYPE_ID = "Claim";
    private static final String OTHER_CASE_TYPE_ID = "OtherClaim";
    private static final String CASE_REFERENCE = "1234123412341236";
    private static final String INVALID_CASE_REFERENCE = "fdasfdasfds";
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
    void setUp() {
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
        caseDetails.setData(data);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState(PRE_STATE_ID);
        caseDetails.setLastModified(LAST_MODIFIED);
        caseDetailsBefore = mock(CaseDetails.class);
        postState = new CaseState();
        postState.setId(POST_STATE);
        IdamUser user = new IdamUser();
        user.setId("123");
        doReturn(buildData("filed1", "field2")).when(caseSanitiser).sanitise(caseType, data);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeService).isJurisdictionValid(JURISDICTION_ID, caseType);
        doReturn(eventTrigger).when(eventTriggerService).findCaseEvent(caseType, EVENT_ID);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(caseDetails).when(caseDetailsRepository).lockCase(Long.valueOf(CASE_REFERENCE));
        doReturn(true).when(eventTriggerService).isPreStateValid(PRE_STATE_ID, eventTrigger);
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);
        doReturn(postState).when(caseTypeService).findState(caseType, POST_STATE);
        doReturn(user).when(userRepository).getUser();
        doReturn(caseDetailsBefore).when(caseService).clone(caseDetails);
        given(callbackInvoker.invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            any())).willReturn(aboutToSubmitCallbackResponse);
    }

    @Test
    @DisplayName("should fail if case type not found")
    void shouldFailIfCaseTypeNotFound() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(resourceNotFoundException.getMessage(), startsWith("Case type with id " + CASE_TYPE_ID + " could not be found for jurisdiction " + JURISDICTION_ID));
    }

    @Test
    @DisplayName("should fail if event trigger not found")
    void shouldFailIfEventTriggerNotFound() {
        doReturn(null).when(eventTriggerService).findCaseEvent(caseType, EVENT_ID);

        ValidationException validationException = assertThrows(ValidationException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(validationException.getMessage(), startsWith(EVENT_ID + " is not a known event ID for the specified case type " + CASE_TYPE_ID));
    }

    @Test
    @DisplayName("should fail if case event not valid")
    void shouldFailIfCaseEventNotValid() {
        doReturn(false).when(eventTriggerService).isPreStateValid(caseDetails.getState(), eventTrigger);

        ValidationException validationException = assertThrows(ValidationException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(validationException.getMessage(), startsWith("Pre-state condition is not valid for case with state: " + caseDetails.getState() + "; and event trigger: " + eventTrigger.getId()));
    }

    @Test
    @DisplayName("should fail if case reference is not valid")
    void shouldFailIfCaseReferenceIsNotValid() {
        doReturn(false).when(uidService).validateUID(CASE_REFERENCE);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(badRequestException.getMessage(), startsWith("Case reference is not valid"));
    }

    @Test
    @DisplayName("should fail if case reference is not in number format")
    void shouldFailIfCaseReferenceIsNotNumberFormat() {
        doReturn(true).when(uidService).validateUID(INVALID_CASE_REFERENCE);

        ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            INVALID_CASE_REFERENCE,
            caseDataContent));
        assertThat(resourceNotFoundException.getMessage(), startsWith("Case with reference " + INVALID_CASE_REFERENCE + " could not be found for case type " + CASE_TYPE_ID));
    }

    @Test
    @DisplayName("should fail if case locking returns null")
    void shouldFailIfCaseLockingReturnsNull() {
        doReturn(null).when(caseDetailsRepository).lockCase(Long.valueOf(CASE_REFERENCE));

        ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(resourceNotFoundException.getMessage(), startsWith("Case with reference " + CASE_REFERENCE + " could not be found for case type " + CASE_TYPE_ID));
    }

    @Test
    @DisplayName("should fail if case type id on the locked case is different to requested case type id")
    void shouldFailIfCaseTypeIdOnTheLockedCaseIsDifferentToRequestedOne() {
        caseDetails.setCaseTypeId(OTHER_CASE_TYPE_ID);

        ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> createEventOperation.createCaseEvent(USER_ID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_REFERENCE,
            caseDataContent));
        assertThat(resourceNotFoundException.getMessage(), startsWith("Case with reference " + CASE_REFERENCE + " could not be found for case type " + CASE_TYPE_ID));
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
            caseDataContent);
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

    static Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        Lists.newArrayList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }

}
