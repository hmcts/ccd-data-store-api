package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

class ApprovalStatusValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "ApprovalStatus";
    private static final String ORGANISATION_TO_ADD = "OrganisationToAdd";

    @Mock
    private BaseType numberBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private ApprovalStatusValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(numberBaseType.getType()).thenReturn(NumberValidator.TYPE_ID);
        BaseType.register(numberBaseType);

        validator = new ApprovalStatusValidator();

        caseFieldDefinition = caseField(FIELD_ID).build();
    }

    private CaseFieldDefinitionBuilder caseField(String fieldId) {
        return new CaseFieldDefinitionBuilder(fieldId).withType(ApprovalStatusValidator.TYPE_ID);
    }

    @Test
    void shouldFailOnInvalidApprovalStatus() {
        final JsonNode data = NODE_FACTORY.textNode("5");
        List<ValidationResult> validationResults =
            this.validator.validate("ApprovalStatus", data, caseFieldDefinition);
        assertNotNull(validationResults);
        assertEquals(1, validationResults.size());
        assertEquals("Invalid Approval Status Value, Valid values are 0,1 and 2. "
                + "0 = ‘Not considered’, 1 = ‘Approved’, 2 = ‘Rejected’",
            validationResults.get(0).getErrorMessage());
    }

    @Test
    void shouldValidateOnValidApprovalStatus() {
        final JsonNode data = NODE_FACTORY.textNode("0");
        List<ValidationResult> validationResults =
            this.validator.validate("ApprovalStatus", data, caseFieldDefinition);
        assertNotNull(validationResults);
        assertEquals(0, validationResults.size());
    }

    @Test
    void shouldReturnEmptyErrorListValidateForOtherFieldId() {
        caseFieldDefinition = caseField(ORGANISATION_TO_ADD).build();
        final JsonNode data = NODE_FACTORY.textNode("0");
        List<ValidationResult> validationResults =
            this.validator.validate("OrganisationToAdd", data, caseFieldDefinition);
        assertNotNull(validationResults);
        assertEquals(0, validationResults.size());
    }

    @Test
    void shouldReturnEmptyErrorListValidateForEmptyData() {
        final JsonNode data = NODE_FACTORY.textNode("");
        List<ValidationResult> validationResults =
            this.validator.validate("OrganisationToAdd", data, caseFieldDefinition);
        assertNotNull(validationResults);
        assertEquals(0, validationResults.size());
    }

    @Test
    void shouldReturnEmptyErrorListValidateForNullData() {
        final JsonNode data = null;
        List<ValidationResult> validationResults =
            this.validator.validate("OrganisationToAdd", data, caseFieldDefinition);
        assertNotNull(validationResults);
        assertEquals(0, validationResults.size());
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("Number"), "Type is incorrect");
    }
}
