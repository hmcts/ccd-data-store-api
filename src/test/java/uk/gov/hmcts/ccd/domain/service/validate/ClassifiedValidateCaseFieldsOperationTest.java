package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassifiedValidateCaseFieldsOperationTest {
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String PAGE_ID = "1";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String JURISDICTION_ID = "Probate";
    private static final CaseTypeDefinition caseTypeDefinition = getCaseTypeDefinition();
    private static final Map<String, JsonNode> EMPTY_CASE_DATA = emptyMap();
    private static final Map<String, JsonNode> EMPTY_CASE_DATA_CLASSIFICATION = emptyMap();
    private static final CaseDetails EMPTY_CASE_DETAILS = new CaseDetails();

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Mock
    private MidEventCallback midEventCallback;

    @Mock
    private CaseService caseService;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private SecurityClassificationServiceImpl classificationService;

    @InjectMocks
    private ClassifiedValidateCaseFieldsOperation classifiedValidateCaseFieldsOperation;

    AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);

        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(caseTypeDefinition);
    }


    @Test
    @DisplayName("should Return Empty CaseDetails with Existing Case")
    void shouldReturnEmptyCaseDetailsWithExistingCase() {
        CaseDataContent content = new CaseDataContent();
        content.setCaseReference(CASE_REFERENCE);
        content.setData(emptyMap());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        when(midEventCallback.invoke(anyString(), any(), any())).thenReturn(emptyMap());
        when(caseService.getCaseDetails(anyString(), anyString())).thenReturn(EMPTY_CASE_DETAILS);
        when(caseDataService.getDefaultSecurityClassifications(any(), anyMap(), anyMap()))
            .thenReturn(emptyMap());
        when(classificationService.applyClassification(any(), anyBoolean())).thenReturn(Optional.empty());

        Map<String, JsonNode> result = classifiedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(midEventCallback).invoke(CASE_TYPE_ID, content, PAGE_ID),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> verify(caseService).getCaseDetails(JURISDICTION_ID, CASE_REFERENCE),
            () -> verify(caseDataService).getDefaultSecurityClassifications(caseTypeDefinition, EMPTY_CASE_DATA,
                EMPTY_CASE_DATA_CLASSIFICATION),
            () -> verify(classificationService).applyClassification(EMPTY_CASE_DETAILS, false),
            () -> assertNull(result)
        );
    }

    @Test
    @DisplayName("should Return Empty CaseDetails with Create Case")
    void shouldReturnEmptyCaseDetailsWithCreateCase() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setData(emptyMap());
        caseDetails.setDataClassification(emptyMap());
        CaseDataContent content = new CaseDataContent();
        content.setData(emptyMap());

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        when(midEventCallback.invoke(anyString(), any(), any())).thenReturn(emptyMap());
        when(caseDataService.getDefaultSecurityClassifications(any(), anyMap(), anyMap()))
            .thenReturn(emptyMap());
        when(classificationService.applyClassification(any(), anyBoolean())).thenReturn(Optional.empty());

        Map<String, JsonNode> result = classifiedValidateCaseFieldsOperation.validateCaseDetails(operationContext);

        ArgumentCaptor<CaseDetails> argumentCaseDetail = ArgumentCaptor.forClass(CaseDetails.class);

        assertAll(
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(operationContext),
            () -> verify(midEventCallback).invoke(CASE_TYPE_ID, content, PAGE_ID),
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> verify(caseService, never()).getCaseDetails(JURISDICTION_ID, CASE_REFERENCE),
            () -> verify(caseDataService).getDefaultSecurityClassifications(caseTypeDefinition, EMPTY_CASE_DATA,
                EMPTY_CASE_DATA_CLASSIFICATION),
            () -> verify(classificationService).applyClassification(argumentCaseDetail.capture(),
                ArgumentMatchers.eq(true)),
            () -> assertEquals(CASE_TYPE_ID, argumentCaseDetail.getValue().getCaseTypeId()),
            () -> assertNull(result)
        );
    }

    @Test
    void shouldGetCaseDefinitionTypeThrowsException() {
        when(caseDefinitionRepository.getCaseType(anyString())).thenReturn(null);
        CaseDataContent content = new CaseDataContent();

        OperationContext operationContext = new OperationContext(CASE_TYPE_ID, content, PAGE_ID);

        assertThrows(ValidationException.class,
            () -> classifiedValidateCaseFieldsOperation.validateCaseDetails(operationContext));
    }

    @Test
    void shouldValidateData() {
        Map<String, JsonNode> data = new HashMap<>();
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        CaseDataContent content = new CaseDataContent();

        classifiedValidateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);

        verify(validateCaseFieldsOperation).validateData(data, caseTypeDefinition, content);
    }

    @Test
    void shouldThrowValidateException() {
        Map<String, JsonNode> data = new HashMap<>();
        CaseDataContent content = new CaseDataContent();

        doThrow(new ValidationException("Validation failed")).when(validateCaseFieldsOperation)
            .validateData(any(), any(), any());

        ValidationException exception = assertThrows(ValidationException.class, () ->
            classifiedValidateCaseFieldsOperation.validateData(data, caseTypeDefinition, content));
        assertEquals("Validation failed", exception.getMessage());
    }

    private static @NotNull CaseTypeDefinition getCaseTypeDefinition() {
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(JURISDICTION_ID);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        return caseTypeDefinition;
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
