package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.types.PostCodeValidator.TYPE_ID;

public class PostCodeValidatorTest extends BaseTest implements IVallidatorTest {
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"PostCode\"\n" +
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
    private PostCodeValidator validator;
    private CaseField caseField;
    private CaseField caseFieldTestMinMax;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        caseFieldTestMinMax = MAPPER.readValue(TEST_CASE_FIELD_STRING_MIN_MAX, CaseField.class);

        final BaseType baseType = validator.getType();
        ReflectionTestUtils.setField(baseType, "regularExpression",
            "^([A-PR-UWYZ0-9][A-HK-Y0-9][AEHMNPRTVXY0-9]?[ABEHMNPRVWXY0-9]? {1,2}[0-9][ABD-HJLN-UW-Z]{2}|GIR 0AA)$");
    }

    @Test
    public void validPostCodesForBaseRegEx() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"M1 1AA\""), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"N60 1NW\""), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"CR2 6XH\""), caseField);
        assertEquals(result01.toString(), 0, result03.size());

        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"DN55 1PT\""), caseField);
        assertEquals(result04.toString(), 0, result04.size());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"W1A 1HQ\""), caseField);
        assertEquals(result05.toString(), 0, result05.size());

        final List<ValidationResult> result06 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"EC1A 1BB\""), caseField);
        assertEquals(result06.toString(), 0, result06.size());
    }

    @Test
    public void invalidPostCodesForBaseRegEx() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"3321M1 1AA\""), caseField);
        assertEquals(result01.toString(), 1, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"1m1 1m1\""), caseField);
        assertEquals(result02.toString(), 1, result01.size());
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
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("POSTCODE"));
    }

    @Test
    public void testInvalidMin() throws Exception {
        final JsonNode INVALID_MIN = MAPPER.readTree("\"Test\"");
        final List<ValidationResult> validationResults = validator.validate("TEST_FIELD_ID", INVALID_MIN, caseFieldTestMinMax);
        assertEquals("Did not catch min", 1, validationResults.size());
        assertEquals("Post code 'Test' requires minimum length 5", validationResults.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", validationResults.get(0).getFieldId());
    }

    @Test
    public void testInvalidMax() throws Exception {
        final JsonNode INVALID_MAX = MAPPER.readTree("\"Test Test Test\"");
        final List<ValidationResult> validationResults = validator.validate("TEST_FIELD_ID", INVALID_MAX, caseFieldTestMinMax);
        assertEquals("Did not catch max", 1, validationResults.size());
        assertEquals("Post code 'Test Test Test' exceeds maximum length 6", validationResults.get(0).getErrorMessage());
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

    @Test
    public void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("EC1A 1BB".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" needs to be a valid " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] needs to be a valid " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(1), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 needs to be a valid " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true needs to be a valid " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode("EC1A 1BB"), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("EC1A 1BB needs to be a valid " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenValidatingNullNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.nullNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldPass_whenValidatingNulText() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode(null), caseField);
        assertThat(result, empty());
    }
}
