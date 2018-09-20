package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

public class FixedListValidatorTest extends BaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"FixedList\",\n" +
        "    \"fixed_list_items\": [\n" +
        "      {\"code\" : \"AAAAAA\"},\n" +
        "      {\"code\" : \"BBBBBB\"},\n" +
        "      {\"code\" : \"CCCCCC\"}\n" +
        "    ]\n" +
        "  }\n" +
        "}";
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    @Inject
    private FixedListValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", null);
    }

    @Test
    public void validValue() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"AAAAAA\""), caseField);
        assertEquals(0, result01.size());
    }

    @Test
    public void invalidValue() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"DDDD\""), caseField);
        assertEquals(result01.toString(), 1, result01.size());
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, null).size());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("FixedList"));
    }

    @Test
    public void fieldTypeRegEx() throws Exception {
        final CaseField caseFieldWithRegEx = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"FixedList\",\n" +
                "    \"regular_expression\": \"AAAAAA\",\n" +
                "    \"fixed_list_items\": [\n" +
                "      {\"code\" : \"AAAAAA\"},\n" +
                "      {\"code\" : \"BBBBBB\"},\n" +
                "      {\"code\" : \"CCCCCC\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}",
            CaseField.class);
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"AAAAAA\""),
            caseFieldWithRegEx);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"BBBBBB\""),
            caseFieldWithRegEx);
        assertEquals("BBBBBB failed regular expression check", 1, result02.size());
        assertEquals(REGEX_GUIDANCE, result02.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result02.get(0).getFieldId());
    }

    @Test
    public void baseTypeRegEx() throws Exception {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "InvalidRegEx");

        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATE\"\n" +
                "  }\n" +
                "}", CaseField.class);
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("AA"), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals("'AA' failed FixedList Type Regex check: InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

}
