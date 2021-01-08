package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("OrgPolicyCaseAssignedRoleValidator")
class OrgPolicyCaseAssignedRoleValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @Mock
    private CaseRoleRepository caseRoleRepository;

    private OrgPolicyCaseAssignedRoleValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();
        validator = new OrgPolicyCaseAssignedRoleValidator(caseRoleRepository);
    }


    @Test
    @DisplayName("should fail due to incorrect organisation role")
    void failDueToIncorrectOrganisationRole() {

        final CaseFieldDefinition caseFieldDefinition = caseField().build();
        caseFieldDefinition.setId("TEST");
        final JsonNode validValue = NODE_FACTORY.textNode("XXXX");
        ValidationContext validationContext1 = createValidationContext(caseFieldDefinition, validValue);
        final List<ValidationResult> validResult = validator.validate(validationContext1);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(1)),
            () -> assertTrue(validResult.get(0).getErrorMessage()
                .contains("The value XXXX is not a valid organisation role."))
        );
    }

    @Test
    @DisplayName("should fail due to null organisation role")
    void failDueToNullOrganisationRole() {

        final CaseFieldDefinition caseFieldDefinition = caseField().build();
        caseFieldDefinition.setId("TEST");
        final JsonNode validValue = NODE_FACTORY.nullNode();
        ValidationContext validationContext1 = createValidationContext(caseFieldDefinition, validValue);
        final List<ValidationResult> validResult = validator.validate(validationContext1);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(1)),
            () -> assertTrue(validResult.get(0).getErrorMessage()
                .contains("Organisation role cannot have an empty value."))
        );
    }

    @Test
    @DisplayName("should pass organisation role")
    void passOrganisationRole() {

        final Set<String> roles = Collections.singleton("ROLE_1");
        final CaseFieldDefinition caseFieldDefinition = caseField().build();
        caseFieldDefinition.setCaseTypeId("TEST");
        final JsonNode validValue = NODE_FACTORY.textNode("ROLE_1");
        ValidationContext validationContext1 = createValidationContext(caseFieldDefinition, validValue);
        when(caseRoleRepository.getCaseRoles(anyString())).thenReturn(roles);
        final List<ValidationResult> validResult = validator.validate(validationContext1);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(0))
        );
    }

    private ValidationContext createValidationContext(CaseFieldDefinition caseFieldDefinition, JsonNode validValue) {
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("TEST");
        final ValidationContext validationContext = new ValidationContext(caseTypeDefinition, null);
        validationContext.setFieldValue(validValue);
        validationContext.setCaseFieldDefinition(caseFieldDefinition);
        validationContext.setFieldId(FIELD_ID);
        return validationContext;
    }

    @Test
    @DisplayName("should fail test against regular expression")
    void textRegexFail() {
        final CaseFieldDefinition caseFieldDefinition =
            caseField().withRegExp("(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)").build();
        final JsonNode validValue = NODE_FACTORY.textNode("15xxxx00");
        ValidationContext validationContext1 = createValidationContext(caseFieldDefinition, validValue);
        final List<ValidationResult> validResult = validator.validate(validationContext1);

        final JsonNode invalidValue = NODE_FACTORY.textNode("15961077777774840593131");
        ValidationContext validationContext2 = createValidationContext(caseFieldDefinition, invalidValue);
        final List<ValidationResult> invalidResult = validator.validate(validationContext2);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(1)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(1))
        );
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(TextValidator.TYPE_ID);
    }
}
