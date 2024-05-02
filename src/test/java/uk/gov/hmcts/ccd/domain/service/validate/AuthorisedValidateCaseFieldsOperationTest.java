package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.config.JacksonUtils.DATA;


class AuthorisedValidateCaseFieldsOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String PAGE_ID = "1";
    private static final String USER_ROLE_1 = "user-role-1";
    private static final String CASE_REFERENCE = "1234123412341234";


    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @InjectMocks
    private AuthorisedValidateCaseFieldsOperation authorisedValidateCaseFieldsOperation;

    AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(caseTypeDefinition);
    }

    @Test
    @DisplayName("should Return Empty CaseDetails With No Access Profile")
    void shouldReturnEmptyCaseDetailsWithNoAccessProfile() {
        OperationContext operationContext = Mockito.mock(OperationContext.class);
        CaseDataContent content = new CaseDataContent();
        content.setCaseReference(CASE_REFERENCE);
        content.setData(JacksonUtils.convertValueInDataField(new ObjectNode(null)));
        when(operationContext.content()).thenReturn(content);
        when(operationContext.caseTypeId()).thenReturn(CASE_TYPE_ID);
        when(operationContext.pageId()).thenReturn(PAGE_ID);

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> assertNotNull(result),
            () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    @DisplayName("should Return CaseDetails With Access Profile")
    void shouldReturnCaseDetailsWithAccessProfile() {
        OperationContext operationContext = Mockito.mock(OperationContext.class);
        CaseDataContent content = new CaseDataContent();
        content.setCaseReference(CASE_REFERENCE);

        content.setData(JacksonUtils.convertValue(new ObjectNode(null)));
        when(operationContext.content()).thenReturn(content);
        when(operationContext.caseTypeId()).thenReturn(CASE_TYPE_ID);
        when(operationContext.pageId()).thenReturn(PAGE_ID);

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        Set<AccessProfile> accessProfiles = Set.of(AccessProfile.builder().accessProfile(USER_ROLE_1).build());
        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(accessProfiles);

        when(accessControlService.canAccessCaseTypeWithCriteria(any(), any(), any()))
            .thenReturn(true);

        ObjectNode filteredData = new ObjectNode(JSON_NODE_FACTORY);
        filteredData.put("filtered_field1", "filtered_value1");
        filteredData.put("filtered_field2", "filtered_value2");
        when(accessControlService.filterCaseFieldsByAccess(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(filteredData);

        Map<String, JsonNode> result = authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService).getAccessProfilesByCaseReference(CASE_REFERENCE),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> assertNotNull(result),
            () -> assertTrue(result.containsKey(DATA)),
            () -> assertEquals(2, result.get(DATA).size())
        );
    }

    @Test
    @DisplayName("should Apply CaseCreationRoles When Case Not Found")
    void shouldApplyCaseCreationRolesWhenCaseNotFound() {
        OperationContext operationContext = Mockito.mock(OperationContext.class);
        CaseDataContent content = new CaseDataContent();
        content.setCaseReference("");
        content.setData(JacksonUtils.convertValueInDataField(new ObjectNode(null)));
        when(operationContext.content()).thenReturn(content);
        when(operationContext.caseTypeId()).thenReturn(CASE_TYPE_ID);
        when(operationContext.pageId()).thenReturn(PAGE_ID);

        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(Set.of());

        authorisedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(caseAccessService).getCaseCreationRoles(CASE_TYPE_ID),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID)
        );
    }

    @Test
    void shouldGetCaseDefinitionTypeThrowsException() {
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(null);

        OperationContext operationContext = Mockito.mock(OperationContext.class);
        CaseDataContent content = new CaseDataContent();
        when(operationContext.content()).thenReturn(content);
        when(operationContext.caseTypeId()).thenReturn(CASE_TYPE_ID);

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

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
