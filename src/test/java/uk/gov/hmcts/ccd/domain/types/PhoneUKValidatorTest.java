package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.test.CaseFieldBuilder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DisplayName("PhoneUKValidator")
class PhoneUKValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String PHONE_REGEX = "^(((\\+44\\s?\\d{4}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)\\s?\\d{3}\\s?\\d{4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)?)\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?$";
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType phoneUkBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private PhoneUKValidator validator;
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(phoneUkBaseType.getType()).thenReturn(PhoneUKValidator.TYPE_ID);
        when(phoneUkBaseType.getRegularExpression()).thenReturn(PHONE_REGEX);
        BaseType.register(phoneUkBaseType);

        validator = new PhoneUKValidator();

        caseField = caseField().build();
    }

    @Test
    void validPhoneUKForBaseRegex() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("01222 555 555"),
                                                                   caseField);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("(010) 55555555 #2222"),
                                                                   caseField);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("07222 555555"),
                                                                   caseField);
        assertEquals(0, result03.size(), result03.toString());

        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("(07222) 555555"),
                                                                   caseField);
        assertEquals(0, result04.size(), result04.toString());

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("07222555555"),
                                                                   caseField);
        assertEquals(0, result05.size(), result05.toString());
    }

    @Test
    void invalidPhoneUKForBaseRegEx() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("3321M1 1AA"),
                                                                   caseField);
        assertEquals(1, result01.size());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("1m1 1m1"),
                                                                   caseField);
        assertEquals(1, result02.size());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("0800505555"),
                                                                   caseField);
        assertEquals(1, result03.size());
    }

    @Test
    void checkFieldRegex() {
        final CaseField caseField = caseField().withRegExp("^[0-9]*$").build();
        final List<ValidationResult> validResult = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("123456789"),
                                                                      caseField);
        assertEquals(0, validResult.size());

        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID,
                                                                        NODE_FACTORY.textNode("abc123"),
                                                                        caseField);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, caseField).size());
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("PhoneUK"));
    }

    @Test
    void testInvalidMin() {
        final CaseField caseField = caseField().withMin(5).build();

        final JsonNode INVALID_MIN = NODE_FACTORY.textNode("Test");
        final List<ValidationResult> validationResults = validator.validate(FIELD_ID, INVALID_MIN, caseField);
        assertEquals(1, validationResults.size(), "Did not catch min");
        assertEquals("Phone no 'Test' requires minimum length 5", validationResults.get(0).getErrorMessage());
        assertEquals(FIELD_ID, validationResults.get(0).getFieldId());
    }

    @Test
    void testInvalidMax() {
        final CaseField caseField = caseField().withMax(6).build();

        final JsonNode INVALID_MAX = NODE_FACTORY.textNode("Test Test Test");
        final List<ValidationResult> validationResults = validator.validate(FIELD_ID, INVALID_MAX, caseField);
        assertEquals(1, validationResults.size(), "Did not catch max");
        assertEquals("Phone no 'Test Test Test' exceeds maximum length 6", validationResults.get(0).getErrorMessage());
        assertEquals(FIELD_ID, validationResults.get(0).getFieldId());
    }

    @Test
    void testValidMinMaxButNoRegExChecks() {
        final CaseField caseField = caseField().withMin(5)
                                               .withMax(6)
                                               .build();
        // Disable regular expression checks
        when(phoneUkBaseType.getRegularExpression()).thenReturn("^.*$");

        final JsonNode DATA = NODE_FACTORY.textNode("5 & 10");
        final List<ValidationResult> validMinMaxResults = validator.validate(FIELD_ID, DATA, caseField);
        assertEquals(0, validMinMaxResults.size(), validMinMaxResults.toString());
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(PhoneUKValidator.TYPE_ID);
    }
}
