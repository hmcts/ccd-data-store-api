package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

public class EmailValidatorTest extends BaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"Email\"\n" +
        "  }\n" +
        "}";

    @Inject
    private EmailValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", null);
    }

    @Test
    public void validEmail() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test@test.com\""), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test@test\""), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test@test.org\""), caseField);
        assertEquals(result01.toString(), 0, result03.size());

        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test@test.org.uk\""), caseField);
        assertEquals(result04.toString(), 0, result04.size());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test.test@test.com\""), caseField);
        assertEquals(result05.toString(), 0, result05.size());

        final List<ValidationResult> result06 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test_test@test.xxx\""), caseField);
        assertEquals(result06.toString(), 0, result06.size());
    }

    @Test
    public void invalidEmail() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test.test.com\""), caseField);
        assertEquals(result01.toString(), 1, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test.com\""), caseField);
        assertEquals(result02.toString(), 1, result01.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"test@test@test\""), caseField);
        assertEquals(result03.toString(), 1, result03.size());
    }

    @Test
    public void fieldTypeRegEx() throws Exception {
        final String caseFieldString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Email\",\n" +
                "    \"regular_expression\": \"^[a-z]\\\\w*@hmcts.net$\"\n" +
                "  }\n" +
                "}";
        final CaseField regexCaseField = MAPPER.readValue(caseFieldString, CaseField.class);
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate("TEST_FIELD_ID", validValue, regexCaseField);
        assertEquals(validResult.toString(), 0, validResult.size());

        final JsonNode invalidValue = NODE_FACTORY.textNode("9k@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate("TEST_FIELD_ID", invalidValue, regexCaseField);
        assertEquals(invalidResult.toString(), 1, invalidResult.size());
        assertEquals(REGEX_GUIDANCE, invalidResult.get(0).getErrorMessage());
    }

    @Test
    public void baseTypeRegEx() throws IOException {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "\\\\w*@hmcts.net");
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("9k@hmcts.net"), caseField);
        assertEquals(1, result01.size());
        assertEquals(REGEX_GUIDANCE, result01.get(0).getErrorMessage());
    }

    @Test
    public void checkMin() throws IOException {
        final String caseFieldString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Email\",\n" +
                "    \"min\": 13\n" +
                "  }\n" +
                "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);
        final JsonNode validValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> validResult = validator.validate("TEST_FIELD_ID", validValue, caseField);
        assertEquals(validResult.toString(), 0, validResult.size());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate("TEST_FIELD_ID", invalidValue, caseField);
        assertEquals(invalidResult.toString(), 1, invalidResult.size());
        assertEquals("Email 'k9@hmcts.net' requires minimum length 13", invalidResult.get(0).getErrorMessage());
    }

    @Test
    public void checkMax() throws IOException {
        final String caseFieldString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Email\",\n" +
                "    \"max\": 12\n" +
                "  }\n" +
                "}";
        final CaseField caseField = MAPPER.readValue(caseFieldString, CaseField.class);
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate("TEST_FIELD_ID", validValue, caseField);
        assertEquals(validResult.toString(), 0, validResult.size());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate("TEST_FIELD_ID", invalidValue, caseField);
        assertEquals(invalidResult.toString(), 1, invalidResult.size());
        assertEquals("Email 'k99@hmcts.net' exceeds maximum length 12", invalidResult.get(0).getErrorMessage());
    }

    @Test
    public void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not a valid email"));
    }

    @Test
    public void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not a valid email"));
    }

    @Test
    public void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("STrinG".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not a valid email"));
    }

    @Test
    public void shouldFail_whenValidatingNumericNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(1), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 is not a valid email"));
    }

    @Test
    public void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldFail_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode("sjobs@apple.com"), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("sjobs@apple.com is not a valid email"));
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, caseField).size());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("Email"));
    }
}
