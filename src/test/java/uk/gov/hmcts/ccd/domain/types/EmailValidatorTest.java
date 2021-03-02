package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

@DisplayName("EmailValidator")
class EmailValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType emailBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private EmailValidator validator;

    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(emailBaseType.getType()).thenReturn(EmailValidator.TYPE_ID);
        when(emailBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(emailBaseType);

        validator = new EmailValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void validEmail() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.com"),
            caseFieldDefinition);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test"),
            caseFieldDefinition);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.org"),
            caseFieldDefinition);
        assertEquals(0, result03.size(), result01.toString());

        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.org.uk"),
            caseFieldDefinition);
        assertEquals(0, result04.size(), result04.toString());

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.test@test.com"),
            caseFieldDefinition);
        assertEquals(0, result05.size(), result05.toString());

        final List<ValidationResult> result06 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test_test@test.xxx"),
            caseFieldDefinition);
        assertEquals(0, result06.size(), result06.toString());
    }

    @Test
    void invalidEmail() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.test.com"),
            caseFieldDefinition);
        assertEquals(1, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.com"),
            caseFieldDefinition);
        assertEquals(1, result01.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test@test"),
            caseFieldDefinition);
        assertEquals(1, result03.size(), result03.toString());
    }

    @Test
    void fieldTypeRegEx() {
        final CaseFieldDefinition regexCaseFieldDefinition = caseField().withRegExp("^[a-z]\\w*@hmcts.net$").build();
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, regexCaseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("9k@hmcts.net");
        final List<ValidationResult> invalidResult =
                validator.validate(FIELD_ID, invalidValue, regexCaseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals(REGEX_GUIDANCE, invalidResult.get(0).getErrorMessage());
    }

    @Test
    void baseTypeRegEx() {
        when(emailBaseType.getRegularExpression()).thenReturn("\\\\w*@hmcts.net");

        final List<ValidationResult> result01 =
                validator.validate(FIELD_ID, NODE_FACTORY.textNode("9k@hmcts.net"), caseFieldDefinition);
        assertEquals(1, result01.size());
        assertEquals(REGEX_GUIDANCE, result01.get(0).getErrorMessage());
    }

    @Test
    void checkMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(new BigDecimal(13)).build();
        final JsonNode validValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals("Email 'k9@hmcts.net' requires minimum length 13", invalidResult.get(0).getErrorMessage());
    }

    @Test
    void checkMax() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(new BigDecimal(12)).build();
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals("Email 'k99@hmcts.net' exceeds maximum length 12", invalidResult.get(0).getErrorMessage());
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, caseFieldDefinition).size(), "Did not catch NULL");
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("Email"), "Type is incorrect");
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(EmailValidator.TYPE_ID);
    }
}
