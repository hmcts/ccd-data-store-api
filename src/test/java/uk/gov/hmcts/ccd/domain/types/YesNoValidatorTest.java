package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Before;
import org.junit.Test;
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
import static uk.gov.hmcts.ccd.domain.types.YesNoValidator.TYPE_ID;

public class YesNoValidatorTest extends BaseTest implements IVallidatorTest {
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"YesOrNo\"\n" +
        "  }\n" +
        "}";

    @Inject
    private YesNoValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
    }

    @Test
    public void correctValue() throws Exception {
        final JsonNode UPPER_YES = MAPPER.readTree("\"YES\"");
        final JsonNode LOWER_YES = MAPPER.readTree("\"yes\"");
        final JsonNode UPPER_NO = MAPPER.readTree("\"NO\"");
        final JsonNode LOWER_NO = MAPPER.readTree("\"no\"");

        assertEquals("YES should be valid", 0, validator.validate("TEST_FIELD_ID", UPPER_YES, caseField).size());
        assertEquals("NO should be valid", 0, validator.validate("TEST_FIELD_ID", UPPER_NO, caseField).size());
        assertEquals("yes should be valid", 0, validator.validate("TEST_FIELD_ID", LOWER_YES, caseField).size());
        assertEquals("no should be valid", 0, validator.validate("TEST_FIELD_ID", LOWER_NO, caseField).size());
    }

    @Test
    public void incorrectValue() throws Exception {
        final JsonNode ANYTHING = MAPPER.readTree("\"dasdahsaAAA\"");
        assertEquals("Did not catch non YES/NO", 1, validator.validate("TEST_FIELD_ID", ANYTHING, caseField).size());
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, caseField).size());
    }

    @Test
    public void nullNode() throws IOException {
        final JsonNode NULL_NODE = NullNode.getInstance();
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", NULL_NODE, caseField).size());
    }

    @Test
    public void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("Yes".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldPass_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode("Yes"), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("Yes is not " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(1), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 is not " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenValidatingNullNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.nullNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldPass_whenValidatingNullValue() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode(null), caseField);
        assertThat(result, empty());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("YesOrNo"));
    }
}
