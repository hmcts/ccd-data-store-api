package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.domain.service.validate.CaseDataIssueLogger;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;

class DefaultCreateCaseOperationTest {

    @Mock
    private CaseStateDefinition caseEventState;
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
    @Mock
    private DraftGateway draftGateway;
    @Mock
    private CaseDataIssueLogger caseDataIssueLogger;
    @Mock
    private GlobalSearchProcessorService globalSearchProcessorService;

    @Mock
    private CasePostStateService casePostStateService;

    @Mock
    private SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    @Mock
    private SupplementaryDataUpdateRequestValidator supplementaryDataValidator;

    @Mock
    private CaseLinkService caseLinkService;

    @Mock
    private ApplicationParams applicationParams;

    private ObjectMapper objectMapper = new ObjectMapper();

    private TimeToLiveService timeToLiveService;

    private DefaultCreateCaseOperation defaultCreateCaseOperation;

    private static final String UID = "244";
    private static final String JURISDICTION_ID = "jid";
    private static final String CASE_TYPE_ID = "cti";
    private final Event event = buildEvent();
    private CaseDataContent eventData = newCaseDataContent().build();

    private static Map<String, JsonNode> data;

    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String TOKEN = "toke";
    private static final String DRAFT_ID = "1";
    private static final String CASE_ID = "123";

    private static final IdamUser IDAM_USER = buildIdamUser();
    private static CaseTypeDefinition CASE_TYPE;
    private CaseEventDefinition eventTrigger;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        timeToLiveService = new TimeToLiveService(objectMapper, applicationParams, caseDataService);
        defaultCreateCaseOperation = new DefaultCreateCaseOperation(userRepository,
                                                                    caseDefinitionRepository,
                                                                    eventTriggerService,
                                                                    eventTokenService,
                                                                    caseDataService,
                                                                    submitCaseTransaction,
                                                                    caseSanitiser,
                                                                    caseTypeService,
                                                                    callbackInvoker,
                                                                    validateCaseFieldsOperation,
                                                                    casePostStateService,
                                                                    draftGateway,
                                                                    caseDataIssueLogger,
                                                                    globalSearchProcessorService,
                                                                    supplementaryDataUpdateOperation,
                                                                    supplementaryDataValidator,
                                                                    caseLinkService,
                                                                    timeToLiveService);
        data = buildJsonNodeData();
        objectMapper.registerModule(new JavaTimeModule());
        given(userRepository.getUser()).willReturn(IDAM_USER);
        given(userRepository.getUserId()).willReturn(UID);
        eventTrigger = newCaseEvent().withId("eventId").withName("event Name").build();
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(DRAFT_ID).build();
        CASE_TYPE = buildCaseType();

        SupplementaryData supplementaryData = new SupplementaryData();
        when(supplementaryDataUpdateOperation.updateSupplementaryData(anyString(), anyObject()))
            .thenReturn(supplementaryData);
    }

    @Test
    @DisplayName("Should throws ValidationException when event is null")
    void shouldThrowValidationException_whenEventIsNull() {
        eventData.setEvent(null);
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "Cannot create case because of event is not specified");
    }

    @Test
    @DisplayName("Should throws ValidationException when event id is null")
    void shouldThrowValidationException_whenEventIdIsNull() {
        eventData.setEvent(anEvent().withEventId(null).build());
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "Cannot create case because of event is not specified");
    }

    @Test
    @DisplayName("Should throws ValidationException when case type is not found")
    void shouldThrowValidationException_whenCaseTypeIsNotFound() {
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "Cannot find case type definition for cti");
    }

    @Test
    @DisplayName("Should throws ValidationException when case type is not defined for Jurisdiction")
    void shouldThrowValidationException_whenCaseTypeIsNotDefinedForJurisdiction() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "Cannot create case because of cti is not defined as case type for jid");
    }

    @Test
    @DisplayName("Should throws ValidationException when event Trigger pres states is null")
    void shouldThrowValidationException_whenEventTriggerPreStatesIsNUll() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class,
            () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                eventData,
                IGNORE_WARNING),
            "Cannot create case because of cti is not defined as case type for jid");
    }


    @Test
    @DisplayName("Should throws ValidationException when event Trigger pre state is null")
    void shouldThrowValidationException_whenEventTriggerPreStateIsNUll() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        assertThrows(ValidationException.class,
            () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                eventData,
                IGNORE_WARNING),
            "Cannot create case because of cti is not defined as case type for jid");
    }


    @Test
    @DisplayName("Should throws ValidationException when event id is not known for case type")
    void shouldThrowValidationException_whenEventIdIsUnknown() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "eid is not a known event ID for the specified case type cti");
    }

    @Test
    @DisplayName("Should throws ValidationException when event pre-state is invalid")
    void shouldThrowValidationException_whenPreStateIsInvalid() {
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.FALSE);
        assertThrows(ValidationException.class, () -> defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                                   eventData,
                                                                                                   IGNORE_WARNING),
                     "Cannot create case because of eventId has pre-states defined");
    }

    @Test
    @DisplayName("Should not try to delete draft if no draft id set")
    void shouldNotTryToDeleteDraftIfNoDraftIdSet() {
        final String caseEventStateId = "Some state";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(null).build();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
                                               same(CASE_TYPE),
                                               same(IDAM_USER),
                                               same(eventTrigger),
                                               any(CaseDetails.class),
                                               same(IGNORE_WARNING),
                                               any()))
            .willReturn(savedCaseType);

        defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                     eventData,
                                                     IGNORE_WARNING);

        verify(draftGateway, never()).delete(DRAFT_ID);
    }

    @Test
    @DisplayName("Should call update supplementary data")
    void shouldCallSaveSupplementaryDataWhenValidDataPassed() {
        final String caseEventStateId = "Some state";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(null).build();
        eventData.setSupplementaryDataRequest(createSupplementaryDataRequest());
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(savedCaseType.getReferenceAsString()).willReturn("1234567");
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);

        defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
            eventData,
            IGNORE_WARNING);

        verify(draftGateway, never()).delete(DRAFT_ID);
        verify(supplementaryDataUpdateOperation)
            .updateSupplementaryData(anyString(), any(SupplementaryDataUpdateRequest.class));
    }

    @Test
    @DisplayName("Should not call update supplementary data")
    void shouldNotCallSaveSupplementaryDataWhenValidDataPassed() {
        final String caseEventStateId = "Some state";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(null).build();
        eventData.setSupplementaryDataRequest(null);
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);

        defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
            eventData,
            IGNORE_WARNING);

        verify(draftGateway, never()).delete(DRAFT_ID);
        verify(supplementaryDataUpdateOperation, never())
            .updateSupplementaryData(anyString(), any(SupplementaryDataUpdateRequest.class));
    }

    @Test
    @DisplayName("Should updateCaseDetailsWithTtlIncrement")
    void shouldUpdateCaseDetailsWithTtlIncrement() {
        final String caseEventStateId = "Some state";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(null).build();
        eventData.setSupplementaryDataRequest(null);
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(caseSanitiser.sanitise(eq(CASE_TYPE), anyMap())).willReturn(data);

        // SETUP TTL
        CaseFieldDefinition ttlDefinition = new CaseFieldDefinition();
        ttlDefinition.setId("TTL");
        List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
        caseFieldDefinitions.addAll(CASE_TYPE.getCaseFieldDefinitions());
        caseFieldDefinitions.add(ttlDefinition);
        CASE_TYPE.setCaseFieldDefinitions(caseFieldDefinitions);
        eventTrigger.setTtlIncrement(3);
        given(caseDataService.getDefaultSecurityClassifications(eq(CASE_TYPE), anyMap(), anyMap()))
            .willAnswer(new Answer<Map<String, JsonNode>>() {
                @Override
                public Map<String, JsonNode> answer(InvocationOnMock invocation) throws Throwable {
                    return (Map<String, JsonNode>) invocation.getArguments()[1];
                }
            });
        given(applicationParams.getTtlGuard()).willReturn(2);

        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any())).willAnswer(new Answer<CaseDetails>() {
                @Override
                public CaseDetails answer(InvocationOnMock invocation) throws Throwable {
                    CaseDetails caseDetails = (CaseDetails) invocation.getArguments()[4];
                    return caseDetails;
                }
            });

        CaseDetails returnedCaseDetails = defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
            eventData,
            IGNORE_WARNING);

        assertTrue(returnedCaseDetails.getData().containsKey(TTL.TTL_CASE_FIELD_ID));
        assertFalse(returnedCaseDetails.getData().get(TTL.TTL_CASE_FIELD_ID).isEmpty());

    }

    @Test
    @DisplayName("Should updateCaseDetailsWithTtlIncrement_1")
    void shouldUpdateCaseDetailsWithTtlIncrement_1() {
        final String caseEventStateId = "Some state";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(null).build();
        eventData.setSupplementaryDataRequest(null);
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(caseSanitiser.sanitise(eq(CASE_TYPE), anyMap())).willReturn(data);

        // SETUP TTL
        CaseFieldDefinition ttlDefinition = new CaseFieldDefinition();
        ttlDefinition.setId("TTL");
        List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
        caseFieldDefinitions.addAll(CASE_TYPE.getCaseFieldDefinitions());
        caseFieldDefinitions.add(ttlDefinition);
        CASE_TYPE.setCaseFieldDefinitions(caseFieldDefinitions);
        eventTrigger.setTtlIncrement(3);
        given(caseDataService.getDefaultSecurityClassifications(eq(CASE_TYPE), anyMap(), anyMap()))
            .willAnswer(new Answer<Map<String, JsonNode>>() {
                @Override
                public Map<String, JsonNode> answer(InvocationOnMock invocation) throws Throwable {
                    return (Map<String, JsonNode>) invocation.getArguments()[1];
                }
            });
        given(applicationParams.getTtlGuard()).willReturn(2);

        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any())).willAnswer(new Answer<CaseDetails>() {
                @Override
                public CaseDetails answer(InvocationOnMock invocation) throws Throwable {
                    CaseDetails caseDetails = (CaseDetails) invocation.getArguments()[4];
                    return caseDetails;
                }
            });

        CaseDetails returnedCaseDetails = defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
            eventData,
            IGNORE_WARNING);

        assertTrue(returnedCaseDetails.getData().containsKey(TTL.TTL_CASE_FIELD_ID));
        assertFalse(returnedCaseDetails.getData().get(TTL.TTL_CASE_FIELD_ID).isEmpty());

    }

    @Test
    @DisplayName("Should set incomplete_delete_draft when exception thrown during draft delete")
    void shouldSetIncompleteDeleteDraftWhenDeleteDraftThrowsException() {
        final String caseEventStateId = "Some state";
        final String draftId = "testDraftId1";
        eventData = newCaseDataContent().withEvent(event).withToken(TOKEN).withData(data).withDraftId(draftId).build();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData)).willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);
        doThrow(RuntimeException.class).when(draftGateway).delete(draftId);

        defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID, eventData, IGNORE_WARNING);
        verify(savedCaseType, times(1)).setIncompleteDeleteDraftResponse();
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
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);
        willDoNothing().given(draftGateway).delete(DRAFT_ID);
        eventTrigger.setCallBackURLSubmittedEvent("   ");

        final CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                     eventData,
                                                                                     IGNORE_WARNING);

        final InOrder order = inOrder(eventTokenService,
                                      caseTypeService,
                                      globalSearchProcessorService,
                                      validateCaseFieldsOperation,
                                      submitCaseTransaction,
                                      draftGateway);
        final ArgumentCaptor<CaseDetails> caseDetailsArgumentCaptor = ArgumentCaptor.forClass(CaseDetails.class);


        assertAll(
            () -> assertThat(caseDetails, IsInstanceOf.instanceOf(CaseDetails.class)),
            () -> order.verify(eventTokenService).validateToken(TOKEN, UID, eventTrigger,
                CASE_TYPE.getJurisdictionDefinition(), CASE_TYPE),
            () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(CASE_TYPE_ID, eventData),
            () -> order.verify(globalSearchProcessorService).populateGlobalSearchData(CASE_TYPE, eventData.getData()),
            () -> order.verify(submitCaseTransaction).submitCase(same(event),
                same(CASE_TYPE),
                same(IDAM_USER),
                same(eventTrigger),
                caseDetailsArgumentCaptor.capture(),
                same(IGNORE_WARNING),
                any()),
            () -> order.verify(draftGateway).delete(DRAFT_ID),
            () -> verifyZeroInteractions(callbackInvoker),
            () -> assertCaseDetails(caseDetailsArgumentCaptor.getValue()),
            () -> assertThat(caseDetails, is(savedCaseType))
        );
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
        given(callbackInvoker.invokeSubmittedCallback(eventTrigger, null, savedCaseType))
            .willThrow(new CallbackException("call back exception"));
        given(
            validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData))
            .willReturn(data);
        given(
            submitCaseTransaction.submitCase(
                same(event),
                same(CASE_TYPE),
                same(IDAM_USER),
                same(eventTrigger),
                any(CaseDetails.class),
                same(IGNORE_WARNING),
                any()))
            .willReturn(savedCaseType);

        defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                     eventData,
                                                     IGNORE_WARNING);

        final InOrder order = inOrder(
            eventTokenService,
            caseTypeService,
            globalSearchProcessorService,
            validateCaseFieldsOperation,
            submitCaseTransaction,
            callbackInvoker,
            savedCaseType,
            draftGateway);

        assertAll("case details saved when call back fails",
            () -> order.verify(eventTokenService).validateToken(TOKEN, UID, eventTrigger,
                CASE_TYPE.getJurisdictionDefinition(), CASE_TYPE),
            () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(CASE_TYPE_ID, eventData),
            () -> order.verify(globalSearchProcessorService).populateGlobalSearchData(CASE_TYPE, eventData.getData()),
            () -> order.verify(submitCaseTransaction).submitCase(same(event),
                same(CASE_TYPE),
                same(IDAM_USER),
                same(eventTrigger),
                any(CaseDetails.class),
                same(IGNORE_WARNING),
                any()),
            () -> order.verify(callbackInvoker).invokeSubmittedCallback(eq(eventTrigger), isNull(CaseDetails.class),
                same(savedCaseType)),
            () -> order.verify(savedCaseType).setIncompleteCallbackResponse(),
            () -> order.verify(draftGateway).delete(DRAFT_ID)
        );
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
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(
            same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);

        final CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                     eventData,
                                                                                     IGNORE_WARNING);

        final InOrder order = inOrder(eventTokenService,
                                      caseTypeService,
                                      validateCaseFieldsOperation,
                                      globalSearchProcessorService,
                                      submitCaseTransaction,
                                      callbackInvoker,
                                      savedCaseType,
                                      draftGateway);

        assertAll("Call back response returned successfully",
            () -> assertThat(caseDetails.getCaseTypeId(), is(mockCaseTypeId)),
            () -> order.verify(eventTokenService).validateToken(TOKEN, UID, eventTrigger,
                CASE_TYPE.getJurisdictionDefinition(), CASE_TYPE),
            () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(CASE_TYPE_ID, eventData),
            () -> order.verify(globalSearchProcessorService).populateGlobalSearchData(CASE_TYPE, eventData.getData()),
            () -> order.verify(submitCaseTransaction).submitCase(same(event),
                same(CASE_TYPE),
                same(IDAM_USER),
                same(eventTrigger),
                any(CaseDetails.class),
                same(IGNORE_WARNING),
                any()),
            () -> order.verify(callbackInvoker).invokeSubmittedCallback(eq(eventTrigger), isNull(CaseDetails.class),
                same(savedCaseType)),
            () -> order.verify(savedCaseType).setAfterSubmitCallbackResponseEntity(response),
            () -> order.verify(draftGateway).delete(DRAFT_ID)
        );
    }

    @Test
    @DisplayName("Should insert case links when case is created")
    void shouldInsertCaseLinks() {
        final String caseEventStateId = "Some state";
        final Long caseReference = 1855854166952584L;

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(CASE_TYPE);
        given(caseTypeService.isJurisdictionValid(JURISDICTION_ID, CASE_TYPE)).willReturn(Boolean.TRUE);
        given(eventTriggerService.findCaseEvent(CASE_TYPE, "eid")).willReturn(eventTrigger);
        given(eventTriggerService.isPreStateValid(null, eventTrigger)).willReturn(Boolean.TRUE);
        given(savedCaseType.getState()).willReturn(caseEventStateId);
        given(savedCaseType.getReference()).willReturn(caseReference);
        given(caseTypeService.findState(CASE_TYPE, caseEventStateId)).willReturn(caseEventState);
        eventTrigger.setCallBackURLSubmittedEvent("http://localhost/submittedcallback");
        given(callbackInvoker.invokeSubmittedCallback(eventTrigger,
            null,
            savedCaseType)).willReturn(response);
        given(response.hasBody()).willReturn(true);
        given(response.getBody()).willReturn(responseBody);
        given(response.getStatusCodeValue()).willReturn(200);
        given(savedCaseType.getCaseTypeId()).willReturn(CASE_TYPE_ID);
        given(savedCaseType.getId()).willReturn(CASE_ID);
        given(savedCaseType.getData()).willReturn(data);
        given(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, eventData))
            .willReturn(data);
        given(submitCaseTransaction.submitCase(
            same(event),
            same(CASE_TYPE),
            same(IDAM_USER),
            same(eventTrigger),
            any(CaseDetails.class),
            same(IGNORE_WARNING),
            any()))
            .willReturn(savedCaseType);

        final CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
            eventData,
            IGNORE_WARNING);

        final InOrder order = inOrder(eventTokenService,
            caseTypeService,
            validateCaseFieldsOperation,
            submitCaseTransaction,
            callbackInvoker,
            savedCaseType,
            caseLinkService,
            draftGateway);

        assertAll("Call back response returned successfully",
            () -> assertThat(caseDetails.getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> order.verify(eventTokenService).validateToken(TOKEN, UID, eventTrigger,
                CASE_TYPE.getJurisdictionDefinition(), CASE_TYPE),
            () -> order.verify(validateCaseFieldsOperation).validateCaseDetails(CASE_TYPE_ID, eventData),
            () -> order.verify(submitCaseTransaction).submitCase(same(event),
                same(CASE_TYPE),
                same(IDAM_USER),
                same(eventTrigger),
                any(CaseDetails.class),
                same(IGNORE_WARNING),
                any()),
            () -> order.verify(callbackInvoker).invokeSubmittedCallback(eq(eventTrigger), isNull(CaseDetails.class),
                same(savedCaseType)),
            () -> order.verify(savedCaseType).setAfterSubmitCallbackResponseEntity(response),
            () -> order.verify(caseLinkService).updateCaseLinks(caseDetails, CASE_TYPE.getCaseFieldDefinitions()),
            () -> order.verify(draftGateway).delete(DRAFT_ID)
        );
    }

    private void assertCaseDetails(final CaseDetails details) {
        assertThat(details.getCaseTypeId(), is("cti"));
        assertThat(details.getJurisdiction(), is(JURISDICTION_ID));
    }

    private static Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId("eid");
        event.setDescription("e-desc");
        event.setSummary("e-summ");
        return event;
    }

    private Map<String, JsonNode> buildJsonNodeData() throws IOException {
        final JsonNode node = objectMapper.readTree("{\n"
            + "  \"PersonFirstName\": \"ccd-First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 1\",\n"
            + "    \"AddressLine2\": " + "\"Address Line 2\"\n"
            + "  }\n"
            + "}\n");
        final Map<String, JsonNode> map = new HashMap<>();
        map.put("xyz", node);
        return map;
    }

    private static IdamUser buildIdamUser() {
        final IdamUser properties = new IdamUser();
        properties.setId("pid");
        properties.setEmail("ngitb@hmcts.net");
        properties.setForename("Wo");
        properties.setSurname("Mata");
        return properties;
    }

    private static CaseTypeDefinition buildCaseType() {
        final JurisdictionDefinition j = buildJurisdiction();
        final Version version = new Version();
        version.setNumber(67);
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("caseTypeId");
        caseTypeDefinition.setName("case type name");
        caseTypeDefinition.setJurisdictionDefinition(j);
        caseTypeDefinition.setVersion(version);
        caseTypeDefinition.setCaseFieldDefinitions(List.of(
            new CaseFieldDefinition()
        ));
        return caseTypeDefinition;
    }

    private static JurisdictionDefinition buildJurisdiction() {
        final JurisdictionDefinition j = new JurisdictionDefinition();
        j.setId(JURISDICTION_ID);
        return j;
    }

    private Map<String, Map<String, Object>> createSupplementaryDataRequest() {
        Map<String, Map<String, Object>> requestData = new HashMap<>();
        Map<String, Object> setOperationData = new HashMap<>();
        requestData.put("$set", setOperationData);
        setOperationData.put("orgs_assigned_users.organisationC", 32);
        setOperationData.put("orgs_assigned_users.organisationA", 54);

        return requestData;
    }

}
