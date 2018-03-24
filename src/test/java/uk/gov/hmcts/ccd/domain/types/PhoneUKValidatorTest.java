package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PhoneUKValidatorTest extends BaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "  }\n" +
        "}";

    private static final String TEST_CASE_FIELD_STRING_MIN_MAX =
        "{\n" +
            "  \"id\": \"TEST_FIELD_ID_MINMAX\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"PostCode\",\n" +
            "    \"max\": 6,\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";

    @Inject
    private PhoneUKValidator validator;
    private CaseField caseField;
    private CaseField caseFieldTestMinMax;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        caseFieldTestMinMax = MAPPER.readValue(TEST_CASE_FIELD_STRING_MIN_MAX, CaseField.class);

        final BaseType baseType = validator.getType();
        ReflectionTestUtils.setField(baseType, "regularExpression",
            "^(((\\+44\\s?\\d{4}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)\\s?\\d{3}\\s?\\d{4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)?)\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?$");
    }

    @Test
    public void validPhoneUKForBaseRegex() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"01222 555 555\""), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"(010) 55555555 #2222\""), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"07222 555555\""), caseField);
        assertEquals(result01.toString(), 0, result03.size());

        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"(07222) 555555\""), caseField);
        assertEquals(result04.toString(), 0, result04.size());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"07222555555\""), caseField);
        assertEquals(result05.toString(), 0, result05.size());
    }

    @Test
    public void invalidPhoneUKForBaseRegEx() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"3321M1 1AA\""), caseField);
        assertEquals(1, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"1m1 1m1\""), caseField);
        assertEquals(1, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"0800505555\""), caseField);
        assertEquals(1, result03.size());
    }

    @Test
    public void checkFieldRegex() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"regular_expression\": \"^[0-9]*$\"\n" +
            "  }\n" +
            "}";
        final List<ValidationResult> validResult = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"123456789\""), MAPPER.readValue(caseFieldString, CaseField.class));
        assertEquals(0, validResult.size());

        final List<ValidationResult> invalidResult = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"abc123\""), MAPPER.readValue(caseFieldString, CaseField.class));
        assertEquals(invalidResult.toString(), 1, invalidResult.size());
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, caseField).size());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("PhoneUK"));
    }

    @Test
    public void testInvalidMin() throws Exception {
        final JsonNode INVALID_MIN = MAPPER.readTree("\"Test\"");
        final List<ValidationResult> validationResults = validator.validate("TEST_FIELD_ID", INVALID_MIN, caseFieldTestMinMax);
        assertEquals("Did not catch min", 1, validationResults.size());
        assertEquals("Phone no 'Test' requires minimum length 5", validationResults.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", validationResults.get(0).getFieldId());
    }

    @Test
    public void testInvalidMax() throws Exception {
        final JsonNode INVALID_MAX = MAPPER.readTree("\"Test Test Test\"");
        final List<ValidationResult> validationResults = validator.validate("TEST_FIELD_ID", INVALID_MAX, caseFieldTestMinMax);
        assertEquals("Did not catch max", 1, validationResults.size());
        assertEquals("Phone no 'Test Test Test' exceeds maximum length 6", validationResults.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", validationResults.get(0).getFieldId());
    }

    @Test
    public void testValidMinMaxButNoRegExChecks() throws Exception {
        // Disable regular expression checks
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "^.*$");
        final JsonNode DATA = MAPPER.readTree("\"5 & 10\"");
        final List<ValidationResult> validMinMaxResults = validator.validate("TEST_FIELD_ID", DATA, caseFieldTestMinMax);
        assertEquals(validMinMaxResults.toString(), 0, validMinMaxResults.size());
    }
}
