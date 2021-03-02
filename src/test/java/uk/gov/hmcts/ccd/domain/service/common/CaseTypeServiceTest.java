package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.CaseDataValidator;
import uk.gov.hmcts.ccd.domain.types.ValidationContext;
import uk.gov.hmcts.ccd.domain.types.ValidationResult;
import uk.gov.hmcts.ccd.domain.types.ValidationResultBuilder;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseValidationException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseTypeServiceTest {

    private static final String CASE_TYPE_ID = "caseTypeId";

    private CaseTypeService subject;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDataValidator caseDataValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new CaseTypeService(caseDataValidator, caseDefinitionRepository);
    }

    @Nested
    @DisplayName("State")
    class State {

        @Test
        @DisplayName("happy path finding a state")
        void happyPathFindState() {
            final CaseTypeDefinition c = new CaseTypeDefinition();
            final CaseStateDefinition caseStateDefinition = buildCaseState("ngitb");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), caseStateDefinition));
            final CaseStateDefinition found = subject.findState(c, "ngitb");
            assertThat(found, is(caseStateDefinition));
        }

        @Test
        @DisplayName("cannot find state by id")
        void cannotFindState() {
            final CaseTypeDefinition c = new CaseTypeDefinition();
            c.setId("nOonEhaStimEtodomYcodEreview");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), buildCaseState("ngitw")));
            final ResourceNotFoundException
                exception = assertThrows(ResourceNotFoundException.class, () -> subject.findState(c, "ngitb"));
            assertThat(exception.getMessage(),
                is("No state found with id 'ngitb' for case type 'nOonEhaStimEtodomYcodEreview'"));
        }

        private CaseStateDefinition buildCaseState(final String name) {
            final CaseStateDefinition s = new CaseStateDefinition();
            s.setId(name);
            return s;
        }
    }

    @Nested
    @DisplayName("Get case type")
    class GetCaseTypeDefinition {

        @Test
        @DisplayName("should return case type when case type is found for id")
        void shouldReturnCaseType() {
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinition);

            CaseTypeDefinition result = subject.getCaseType(CASE_TYPE_ID);

            assertThat(result, is(caseTypeDefinition));
            verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        }

        @Test
        @DisplayName("should throw exception when case type is not found")
        void shouldThrowException() {
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> subject.getCaseType(CASE_TYPE_ID));
        }
    }

    @Nested
    @DisplayName("Validate data")
    class ValidateData {

        @Test
        @DisplayName("should call case data validator")
        void shouldCallCaseDataValidator() {

            // ARRANGE
            when(caseDataValidator.validate(any())).thenReturn(new ArrayList<>()); // i.e. no errors

            Map<String, JsonNode> data = new HashMap<>();
            List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);

            // ACT
            subject.validateData(data, caseTypeDefinition);

            // ASSERT
            verify(caseDataValidator,
                times(1)).validate(any(ValidationContext.class));
        }

        @Test
        @DisplayName("should throw exception when validation messages are returned")
        void shouldThrowCaseValidationException() {

            // ARRANGE
            List<ValidationResult> validationResults = new ArrayList<>();
            validationResults.add(new ValidationResultBuilder().setErrorMessage("message 1").setFieldId("field 1")
                .build());
            validationResults.add(new ValidationResultBuilder().setErrorMessage("message 2").setFieldId("field 2")
                .build());

            when(caseDataValidator.validate(any())).thenReturn(validationResults); // i.e. two errors

            Map<String, JsonNode> data = new HashMap<>();
            List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);

            // ASSERT (() -> ACT))
            assertThrows(CaseValidationException.class, () -> subject.validateData(data, caseTypeDefinition));
        }
    }

    private ValidationContext getValidationContext(
        Map<String, JsonNode> values, List<CaseFieldDefinition> caseFieldDefinitions) {

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);
        return new ValidationContext(caseTypeDefinition, values);
    }
}

