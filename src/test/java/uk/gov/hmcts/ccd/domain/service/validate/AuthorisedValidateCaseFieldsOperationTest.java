package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.ConditionalFieldRestorer;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.config.JacksonUtils.DATA;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_STATE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_FIELD_FOUND;

class AuthorisedValidateCaseFieldsOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String PAGE_ID = "1";
    private static final String USER_ROLE_1 = "user-role-1";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String EVENT_ID = "testEvent";

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Mock
    private ConditionalFieldRestorer conditionalFieldRestorer;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private MidEventCallback midEventCallback;

    @Mock
    private GetCaseOperation getCaseOperation;

    private AuthorisedValidateCaseFieldsOperation authorisedValidateCaseFieldsOperation;

    AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        authorisedValidateCaseFieldsOperation = new AuthorisedValidateCaseFieldsOperation(
            accessControlService,
            caseDefinitionRepository,
            caseAccessService,
            validateCaseFieldsOperation,
            conditionalFieldRestorer,
            applicationParams,
            midEventCallback,
            getCaseOperation
        );

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(caseTypeDefinition);

        CaseDetails loadedCase = new CaseDetails();
        loadedCase.setCaseTypeId(CASE_TYPE_ID);
        loadedCase.setState("Open");
        loadedCase.setData(new HashMap<>());
        when(getCaseOperation.execute(eq(CASE_REFERENCE))).thenReturn(Optional.of(loadedCase));

        when(caseAccessService.getCaseCreationRoles(anyString())).thenReturn(
            Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));
        when(caseAccessService.getAccessProfilesByCaseReference(eq(CASE_REFERENCE))).thenReturn(
            Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any())).thenReturn(true);
        when(accessControlService.canAccessCaseEventWithCriteria(anyString(), any(), any(), any())).thenReturn(true);
        when(accessControlService.canAccessCaseFieldsWithCriteria(any(), any(), any(), any())).thenReturn(true);
        when(accessControlService.canAccessCaseStateWithCriteria(anyString(), any(), any(), any())).thenReturn(true);

        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate()).thenReturn(List.of());
    }

    @Test
    @DisplayName("should Skip VerifyAccess When CaseTypeId Is Excluded")
    void shouldSkipVerifyAccessWhenCaseTypeIdIsExcluded() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        Map<String, JsonNode> inputData = new HashMap<>();
        inputData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        content.setData(inputData);

        Map<String, JsonNode> midEventData = new HashMap<>();
        midEventData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(PAGE_ID)))
            .thenReturn(midEventData);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);
        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate())
            .thenReturn(List.of(CASE_TYPE_ID));

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(midEventCallback).invoke(CASE_TYPE_ID, content, PAGE_ID),
            () -> assertNotNull(result),
            () -> assertNotEquals(inputData, result),
            () -> assertTrue(result.containsKey("data")),
            () -> assertEquals("value1", result.get("data").get("field1").asText()),
            () -> verify(caseAccessService).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID)
        );
    }

    @Test
    @DisplayName("should Continue VerifyAccess When CaseTypeId Not Excluded")
    void shouldContinueVerifyAccessWhenCaseTypeIdNotExcluded() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(new HashMap<>());


        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate()).thenReturn(List.of("SomeOtherType"));


        when(caseAccessService.getAccessProfilesByCaseReference(anyString()))
            .thenReturn(Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));


        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any()))
            .thenReturn(true);


        ObjectNode filteredData = new ObjectNode(JSON_NODE_FACTORY);
        filteredData.put("filtered_field1", "filtered_value1");
        when(accessControlService.filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(filteredData);


        when(conditionalFieldRestorer.restoreConditionalFields(any(), any(), any(), any()))
            .thenReturn(JacksonUtils.convertValue(filteredData));


        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);


        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);


        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService, times(2)).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository, times(2)).getCaseType(CASE_TYPE_ID),
            () -> assertNotNull(result),
            () -> assertEquals("filtered_value1", result.get(DATA).get("filtered_field1").asText())
        );
    }

    @Test
    @DisplayName("should reject validate when user has no access profiles for case")
    void shouldRejectValidateWhenUserHasNoAccessProfilesForCase() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        assertThrows(ValidationException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should Return CaseDetails With Access Profile")
    void shouldReturnCaseDetailsWithAccessProfile() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        content.setData(JacksonUtils.convertValue(new ObjectNode(null)));

        when(caseAccessService.getAccessProfilesByCaseReference(anyString()))
            .thenReturn(Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any()))
            .thenReturn(true);

        ObjectNode filteredData = new ObjectNode(JSON_NODE_FACTORY);
        filteredData.put("filtered_field1", "filtered_value1");
        filteredData.put("filtered_field2", "filtered_value2");
        when(accessControlService.filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(filteredData);

        when(conditionalFieldRestorer.restoreConditionalFields(any(), any(), any(), any()))
            .thenReturn(JacksonUtils.convertValue(filteredData));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService, times(2)).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository, times(2)).getCaseType(CASE_TYPE_ID),
            () -> assertNotNull(result),
            () -> assertTrue(result.containsKey(DATA)),
            () -> assertEquals(2, result.get(DATA).size())
        );
    }

    @Test
    @DisplayName("should Return CaseDetails With Restored Missing Field")
    void shouldReturnCaseDetailsWithRestoredMissingField() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        when(caseAccessService.getAccessProfilesByCaseReference(anyString()))
            .thenReturn(Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any()))
            .thenReturn(true);

        ObjectNode filteredData = new ObjectNode(JSON_NODE_FACTORY);
        filteredData.put("filtered_field1", "filtered_value1");
        filteredData.put("filtered_field2", "filtered_value2");

        when(accessControlService.filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(filteredData);

        // Simulate restoring fields (new field is restored here)
        ObjectNode restoredData = new ObjectNode(JSON_NODE_FACTORY);
        restoredData.put("filtered_field1", "filtered_value1");
        restoredData.put("filtered_field2", "filtered_value2");
        restoredData.put("restored_field", "restored_value");

        when(conditionalFieldRestorer.restoreConditionalFields(any(), any(), any(), any()))
            .thenReturn(JacksonUtils.convertValue(restoredData));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService, times(2)).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository, times(2)).getCaseType(CASE_TYPE_ID),
            () -> assertNotNull(result),
            () -> assertTrue(result.containsKey(DATA)),
            () -> assertEquals(3, result.get(DATA).size()),
            () -> assertEquals("restored_value", result.get(DATA).get("restored_field").asText())
        );
    }

    @Test
    @DisplayName("should Apply CaseCreationRoles When Case Not Found")
    void shouldApplyCaseCreationRolesWhenCaseNotFound() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference("");
        content.setData(emptyMap());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService, atLeast(2)).getCaseCreationRoles(CASE_TYPE_ID),
            () -> verify(caseDefinitionRepository, times(2)).getCaseType(CASE_TYPE_ID)
        );
    }

    @Test
    void shouldGetCaseDefinitionTypeThrowsException() {
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(null);

        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        assertThrows(ValidationException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));
    }

    @Test
    void shouldValidateData() {
        Map<String, JsonNode> data = new HashMap<>();
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        CaseDataContent content = new CaseDataContent();

        authorisedValidateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);

        verify(validateCaseFieldsOperation).validateData(data, caseTypeDefinition, content);
    }

    @Test
    @DisplayName("should invoke mid event callback and update content data")
    void shouldInvokeMidEventCallbackAndUpdateContentData() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        Map<String, JsonNode> inputData = new HashMap<>();
        inputData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        content.setData(inputData);

        Map<String, JsonNode> callbackResponseData = new HashMap<>();
        callbackResponseData.put("field1", JSON_NODE_FACTORY.textNode("updatedValue1"));
        callbackResponseData.put("field2", JSON_NODE_FACTORY.textNode("value2"));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate())
            .thenReturn(List.of(CASE_TYPE_ID));
        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(PAGE_ID)))
            .thenReturn(callbackResponseData);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(midEventCallback).invoke(CASE_TYPE_ID, content, PAGE_ID),
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> assertNotNull(result),
            () -> assertTrue(result.containsKey("data")),
            () -> assertEquals(2, result.get("data").size())
        );
    }

    @Test
    @DisplayName("should invoke mid event callback with empty page id")
    void shouldInvokeMidEventCallbackWithEmptyPageId() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        Map<String, JsonNode> inputData = new HashMap<>();
        inputData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        content.setData(inputData);

        Map<String, JsonNode> callbackResponseData = new HashMap<>();
        callbackResponseData.put("field1", JSON_NODE_FACTORY.textNode("value1"));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, "");

        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate())
            .thenReturn(List.of(CASE_TYPE_ID));
        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq("")))
            .thenReturn(callbackResponseData);

        authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        verify(midEventCallback).invoke(CASE_TYPE_ID, content, "");
    }

    @Test
    @DisplayName("should invoke mid event callback with null page id")
    void shouldInvokeMidEventCallbackWithNullPageId() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        Map<String, JsonNode> inputData = new HashMap<>();
        inputData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        content.setData(inputData);

        Map<String, JsonNode> callbackResponseData = new HashMap<>();
        callbackResponseData.put("field1", JSON_NODE_FACTORY.textNode("value1"));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, null);

        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate())
            .thenReturn(List.of(CASE_TYPE_ID));
        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(null)))
            .thenReturn(callbackResponseData);

        authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        verify(midEventCallback).invoke(CASE_TYPE_ID, content, null);
    }

    @Test
    @DisplayName("should invoke mid event callback and preserve data when continuing verify access")
    void shouldInvokeMidEventCallbackAndPreserveDataWhenContinuingVerifyAccess() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        Map<String, JsonNode> inputData = new HashMap<>();
        inputData.put("field1", JSON_NODE_FACTORY.textNode("value1"));
        content.setData(inputData);

        Map<String, JsonNode> callbackResponseData = new HashMap<>();
        callbackResponseData.put("field1", JSON_NODE_FACTORY.textNode("callbackValue1"));
        callbackResponseData.put("field2", JSON_NODE_FACTORY.textNode("callbackValue2"));

        when(applicationParams.getExcludeVerifyAccessCaseTypesForValidate())
            .thenReturn(List.of("SomeOtherType"));
        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(PAGE_ID)))
            .thenReturn(callbackResponseData);
        when(caseAccessService.getAccessProfilesByCaseReference(anyString()))
            .thenReturn(Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build()));
        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any()))
            .thenReturn(true);

        ObjectNode filteredData = new ObjectNode(JSON_NODE_FACTORY);
        filteredData.put("filtered_field1", "filtered_value1");
        when(accessControlService.filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(filteredData);

        when(conditionalFieldRestorer.restoreConditionalFields(any(), any(), any(), any()))
            .thenReturn(JacksonUtils.convertValue(filteredData));

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(midEventCallback).invoke(CASE_TYPE_ID, content, PAGE_ID),
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService, times(2)).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> assertNotNull(result)
        );
    }

    @Test
    @DisplayName("should throw when event is missing before mid event")
    void shouldThrowWhenEventIsMissing() {
        CaseDataContent content = new CaseDataContent();
        content.setCaseReference(CASE_REFERENCE);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_EVENT_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when event id is empty before mid event")
    void shouldThrowWhenEventIdIsEmpty() {
        CaseDataContent content = new CaseDataContent();
        Event event = new Event();
        event.setEventId("");
        content.setEvent(event);
        content.setCaseReference(CASE_REFERENCE);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_EVENT_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when create case user has no roles")
    void shouldThrowWhenCreateCaseUserHasNoRoles() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference("");
        content.setData(emptyMap());

        when(caseAccessService.getCaseCreationRoles(CASE_TYPE_ID)).thenReturn(Set.of());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals("Cannot find user roles for the user", exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when create case type access is denied")
    void shouldThrowWhenCreateCaseTypeAccessDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference("");
        content.setData(emptyMap());

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), eq(CAN_CREATE)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_CASE_TYPE_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when create case event access is denied")
    void shouldThrowWhenCreateCaseEventAccessDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference("");
        content.setData(emptyMap());

        when(accessControlService.canAccessCaseEventWithCriteria(anyString(), any(), any(), eq(CAN_CREATE)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_EVENT_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when create case field access is denied")
    void shouldThrowWhenCreateCaseFieldAccessDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference("");
        content.setData(null);

        when(accessControlService.canAccessCaseFieldsWithCriteria(any(), any(), any(), eq(CAN_CREATE)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_FIELD_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when update case is not found")
    void shouldThrowWhenUpdateCaseNotFound() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.empty());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals("Case not found", exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when update case type access is denied")
    void shouldThrowWhenUpdateCaseTypeAccessDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), eq(CAN_UPDATE)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_CASE_TYPE_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should throw when update case state access is denied")
    void shouldThrowWhenUpdateCaseStateAccessDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        when(accessControlService.canAccessCaseStateWithCriteria(anyString(), any(), any(), eq(CAN_UPDATE)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        assertEquals(NO_CASE_STATE_FOUND, exception.getMessage());
        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    @Test
    @DisplayName("should return empty data when read access to case type is denied")
    void shouldReturnEmptyDataWhenReadAccessToCaseTypeIsDenied() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(Map.of("field1", JSON_NODE_FACTORY.textNode("value1")));

        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(PAGE_ID)))
            .thenReturn(Map.of("field1", JSON_NODE_FACTORY.textNode("value1")));
        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), eq(CAN_READ)))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertTrue(result.containsKey(DATA));
        assertTrue(result.get(DATA).isEmpty());
        verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("should return empty data when content data is null after mid event")
    void shouldReturnEmptyDataWhenContentDataIsNullAfterMidEvent() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);

        when(midEventCallback.invoke(eq(CASE_TYPE_ID), eq(content), eq(PAGE_ID))).thenReturn(null);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertTrue(result.containsKey(DATA));
        assertTrue(result.get(DATA).isEmpty());
        verify(accessControlService, never()).filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("should not invoke mid event when user lacks case event access")
    void shouldNotInvokeMidEventWhenUserLacksCaseEventAccess() {
        CaseDataContent content = new CaseDataContent();
        attachEvent(content);
        content.setCaseReference(CASE_REFERENCE);
        content.setData(new HashMap<>());

        when(accessControlService.canAccessCaseEventWithCriteria(anyString(), any(), any(), any()))
            .thenReturn(false);

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        assertThrows(ResourceNotFoundException.class,
            () -> authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext));

        verify(midEventCallback, never()).invoke(anyString(), any(), any());
    }

    private static void attachEvent(CaseDataContent content) {
        Event event = new Event();
        event.setEventId(EVENT_ID);
        content.setEvent(event);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
