package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

public class NumberValidatorTest extends BaseTest implements IVallidatorTest {
    private static final String
        SIMPLE_CASE_FIELD_STRING =
        "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\"\n" +
            "  }\n" +
            "}";

    private NumberValidator validator = new NumberValidator();

    @Before
    public void setup() {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", null);
    }

    @Test
    public void noValueOrMaxOrMin() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);

        final JsonNode data = MAPPER.readTree("\"\"");
        assertEquals(validator.validate("TEST_FIELD_ID", data, caseField).toString(), 0, validator.validate("TEST_FIELD_ID", data, caseField).size());
    }

    @Test
    public void noValueWithMaxOrMin() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\",\n" +
            "    \"max\": 10,\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);
        final JsonNode data = MAPPER.readTree("\"\"");
        assertEquals(0, validator.validate("TEST_FIELD_ID", data, caseField).size());
    }

    @Test
    public void valueWithMaxMin() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\",\n" +
            "    \"max\": 10,\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        assertEquals("5 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("5"), caseField).size());

        assertEquals("5 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(5), caseField).size());

        assertEquals("5.001 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("5.001"), caseField).size());

        assertEquals("5.001 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(5.001), caseField).size());

        assertEquals("9.999999 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("9.999999"), caseField).size());

        assertEquals("9.999999 should be with in range of between 5 and 10", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(9.999999), caseField).size());

        List<ValidationResult> textNodeBelowMin = validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("4.9"), caseField);
        assertEquals("4.9 should not be with in range of between 5 and 10", 1, textNodeBelowMin.size());
        assertEquals("Should be more than or equal to 5", textNodeBelowMin.get(0).getErrorMessage());

        List<ValidationResult> numberNodeBelowMin = validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(4.9), caseField);
        assertEquals("4.9 should not be with in range of between 5 and 10", 1, numberNodeBelowMin.size());
        assertEquals("Should be more than or equal to 5", numberNodeBelowMin.get(0).getErrorMessage());

        List<ValidationResult> textNodeAboveMin = validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("10.1"), caseField);
        assertEquals("10.1 should not be with in range of between 5 and 10", 1, textNodeAboveMin.size());
        assertEquals("Should be less than or equal to 10", textNodeAboveMin.get(0).getErrorMessage());

        List<ValidationResult> numberNodeAboveMin = validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(10.1), caseField);
        assertEquals("10.1 should not be with in range of between 5 and 10", 1,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(10.1), caseField).size());
        assertEquals("Should be less than or equal to 10", numberNodeAboveMin.get(0).getErrorMessage());
    }

    @Test
    public void valueWithSameMaxMin() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\",\n" +
            "    \"max\": 0,\n" +
            "    \"min\": 0\n" +
            "  }\n" +
            "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        assertEquals("0 should be with in range of between 0 and 0", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0"), caseField).size());

        assertEquals("0 should be with in range of between 0 and 0", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0), caseField).size());

        assertEquals("-1 should not be with in range of between 0 and 0", 1,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("-1"), caseField).size());

        assertEquals("-1 should not be with in range of between 0 and 0", 1,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(-1), caseField).size());

        assertEquals("0.0000000000 should be with in range of between 0 and 0", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0.0000000000"), caseField).size());

        assertEquals("0.0000000000 should be with in range of between 0 and 0", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0.0000000000), caseField).size());
    }

    @Test
    public void valueWithSameDecimalMaxMin() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\",\n" +
            "    \"max\": 0.00,\n" +
            "    \"min\": 0.00\n" +
            "  }\n" +
            "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        assertEquals("0 should be with in range of between 0.00 and 0.00", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0"), caseField).size());

        assertEquals("0 should be with in range of between 0.00 and 0.00", 0,
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0), caseField).size());
    }

    @Test
    public void fieldTypeRegEx() throws Exception {
        final String caseFieldString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Number\",\n" +
                "    \"regular_expression\": \"^\\\\d\\\\.\\\\d\\\\d$\"\n" +
                "  }\n" +
                "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        assertEquals("regular expression check", 0,
            validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"8.20\""), caseField).size());

        List<ValidationResult> results = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"8.2\""), caseField);
        assertEquals("regular expression check", 1, results.size());
        assertEquals(REGEX_GUIDANCE, results.get(0).getErrorMessage());
    }

    @Test
    public void invalidBaseTypeRegEx() throws Exception {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "\\d");

        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(12), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals("'12' failed number Type Regex check: \\d", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

    @Test
    public void incorrectFormat() throws Exception {
        final String caseFieldString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Number\",\n" +
            "    \"max\": \"10\",\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        assertEquals("Did not catch invalid 10.1xxxx", 1,
            validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"10.1xxxx\""), caseField).size());
    }

    @Test(expected = JsonParseException.class)
    public void incorrectFormatAsNumber() throws Exception {
        final String caseFieldString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Number\",\n" +
                "    \"max\": \"10\",\n" +
                "    \"min\": 5\n" +
                "  }\n" +
                "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);

        validator.validate("TEST_FIELD_ID", MAPPER.readTree("10.1xxxx"), caseField);
    }

    @Test
    public void shouldFailWhenValidatingAString() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);

        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("PkJiTQ"), caseField);
        assertThat(validationResults, hasSize(1));
        assertThat(validationResults.get(0).getErrorMessage(), is("PkJiTQ is not a number"));
    }

    @Test
    public void shouldFailWhenValidatingABoolean() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);

        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(validationResults, hasSize(1));
        assertThat(validationResults.get(0).getErrorMessage(), is("true is not a number"));
    }

    @Test
    public void shouldFailWhenValidatingBinaryData() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);

        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("ngitb".getBytes()), caseField);
        assertThat(validationResults, hasSize(1));
        assertThat(validationResults.get(0).getErrorMessage(), endsWith(" is not a number"));
    }

    @Test
    public void shouldFailWhenValidatingArrayNode() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);
        final JsonNode node = NODE_FACTORY.arrayNode();
        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", node, caseField);
        assertThat(validationResults, hasSize(1));
        assertThat(validationResults.get(0).getErrorMessage(), endsWith(" is not a number"));
    }

    @Test
    public void shouldPassWhenValidatingObjectNode() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);
        final JsonNode node = NODE_FACTORY.objectNode();
        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", node, caseField);
        assertThat(validationResults, empty());
    }

    @Test
    public void shouldFailWhenValidatingPoJoNode() throws Exception {
        final CaseField caseField = MAPPER.readValue(SIMPLE_CASE_FIELD_STRING, CaseField.class);
        final JsonNode node = NODE_FACTORY.pojoNode(1);
        final List<ValidationResult>
            validationResults =
            validator.validate("TEST_FIELD_ID", node, caseField);
        assertThat(validationResults, hasSize(1));
        assertThat(validationResults.get(0).getErrorMessage(), is("1 is not a number"));
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, null).size());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("NUMBER"));
    }
}
