package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class DefaultValidateCaseFieldsOperationTest {

    private static final String CASE_TYPE_ID = "caseTypeId";
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private FieldProcessorService fieldProcessorService;
    @Mock
    private CaseType caseType;
    @Mock
    private CaseDataContent caseDataContent;

    private String eventId = "eventId";
    private Map<String, JsonNode> data = Maps.newHashMap();

    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(caseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseType).hasEventId(eventId);

        doReturn(data).when(caseDataContent).getData();
        Event event = anEvent().withEventId(eventId).build();
        doReturn(event).when(caseDataContent).getEvent();
        doReturn(eventId).when(caseDataContent).getEventId();

        validateCaseFieldsOperation = new DefaultValidateCaseFieldsOperation(caseDefinitionRepository, caseTypeService, fieldProcessorService);
    }

    @Test
    void shouldValidateCaseDetails() {
        Map<String, JsonNode> result = validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> verify(caseTypeService).validateData(data, caseType),
            () -> assertThat(result, is(data))
        );
    }

    @Test
    void shouldFailValidationIfNoContent() {
        ValidationException exception = assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, null));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventInContent() {
        doReturn(null).when(caseDataContent).getEvent();

        ValidationException exception = assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventIdInContent() {
        doReturn(null).when(caseDataContent).getEventId();

        ValidationException exception = assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoCaseType() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        ValidationException exception = assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot find case type definition for " + CASE_TYPE_ID));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfEventIdNotFoundInCaseType() {
        doReturn("otherEvent").when(caseDataContent).getEventId();

        ValidationException exception = assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event otherEvent is not found in case type definition"));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }
}
