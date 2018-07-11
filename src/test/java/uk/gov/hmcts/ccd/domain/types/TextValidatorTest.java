package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TextValidatorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"Text\",\n" +
        "    \"max\": 10,\n" +
        "    \"min\": 5\n" +
        "  }\n" +
        "}";

    private TextValidator validator;
    private CaseField caseField;

    @BeforeEach
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);

        validator = new TextValidator();
    }

    @Test
    @DisplayName("should be valid when text length between min and max")
    public void textFieldWithValidMinMax() throws Exception {
        final JsonNode DATA = MAPPER.readTree("\"5 & 10\"");
        final List<ValidationResult> validMinMaxResults = validator.validate("TEST_FIELD_ID", DATA, caseField);

        assertThat(validMinMaxResults.toString(), validMinMaxResults, hasSize(0));
    }

    @Test
    @DisplayName("should NOT be valid when text length outside of min and max")
    public void textFieldWithInvalidMinMax() throws Exception {
        final JsonNode INVALID_MIN = MAPPER.readTree("\"Test\"");
        final List<ValidationResult> validMinResults = validator.validate("TEST_FIELD_ID", INVALID_MIN, caseField);

        final JsonNode INVALID_MAX = MAPPER.readTree("\"Test Test Test\"");
        final List<ValidationResult> validMaxResults = validator.validate("TEST_FIELD_ID", INVALID_MAX, caseField);

        assertAll(
            () -> assertThat("Min not catched", validMinResults, hasSize(1)),
            () -> assertThat("Max not catched", validMaxResults, hasSize(1)),
            () -> assertThat(validMinResults, hasItem(
                    hasProperty("errorMessage", equalTo("Test require minimum length 5")))),
            () -> assertThat(validMinResults, hasItem(hasProperty("fieldId", equalTo("TEST_FIELD_ID")))),
            () -> assertThat(validMaxResults, hasItem(
                    hasProperty("errorMessage", equalTo("Test Test Test exceed maximum length 10")))),
            () -> assertThat(validMaxResults, hasItem(hasProperty("fieldId", equalTo("TEST_FIELD_ID"))))
        );
    }

    @Test
    @DisplayName("should be valid when no min and max defined")
    public void textFieldWithNoMinMax() throws Exception {
        final String NO_MAX_MIN_CASE_FIELD_STRING =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Text\"\n" +
            "  }\n" +
            "}";
        final CaseField noMinMaxCaseField = MAPPER.readValue(NO_MAX_MIN_CASE_FIELD_STRING, CaseField.class);
        final JsonNode value = MAPPER.readTree("\"Test\"");
        final List<ValidationResult> validMinMaxResults = validator.validate("TEST_FIELD_ID", value, noMinMaxCaseField);
        assertThat(validMinMaxResults.toString(), validMinMaxResults, hasSize(0));
    }

    @Test
    @DisplayName("should test exact length when min and max are equal")
    public void textFieldWithSameMinMax() throws Exception {
        final String SAME_MAX_MIN_CASE_FIELD_STRING =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Text\",\n" +
            "    \"max\": 5,\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";
        final CaseField sameMinMaxCaseField = MAPPER.readValue(SAME_MAX_MIN_CASE_FIELD_STRING, CaseField.class);
        final JsonNode valid_value = MAPPER.readTree("\"12345\"");
        final List<ValidationResult> validMinMaxResults = validator.validate("TEST_FIELD_ID", valid_value, sameMinMaxCaseField);

        // Test value over
        final JsonNode over_value = MAPPER.readTree("\"123456\"");
        final List<ValidationResult> overMinMaxResults = validator.validate("TEST_FIELD_ID", over_value, sameMinMaxCaseField);

        // Test value under
        final JsonNode under_value = MAPPER.readTree("\"1234\"");
        final List<ValidationResult> underMinMaxResults = validator.validate("TEST_FIELD_ID", under_value, sameMinMaxCaseField);

        assertAll(
            () -> assertThat("Expected valid input", validMinMaxResults, hasSize(0)),
            () -> assertThat("Under allowed minimum", underMinMaxResults, hasSize(1)),
            () -> assertThat("Over allowed maximum", overMinMaxResults, hasSize(1))
        );
    }

    @Test
    @DisplayName("should test against regular expression")
    public void textRegex() throws Exception {
        final String REGEX_CASE_FIELD_STRING =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"Text\",\n" +
            "    \"regular_expression\": \"\\\\d{4}-\\\\d{2}-\\\\d{2}\"\n" +
            "  }\n" +
            "}";
        final CaseField regexCaseField = MAPPER.readValue(REGEX_CASE_FIELD_STRING, CaseField.class);
        final JsonNode validValue = MAPPER.readTree("\"1234-56-78\"");
        final List<ValidationResult> validResult = validator.validate("TEST_FIELD_ID", validValue, regexCaseField);

        final JsonNode invalidValue = MAPPER.readTree("\"aa-56-78\"");
        final List<ValidationResult> invalidResult = validator.validate("TEST_FIELD_ID", invalidValue, regexCaseField);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(0)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(1))
        );
    }

    @Test
    @DisplayName("should be linked to base type")
    @Disabled("Use of static base type is preventing the use of a mock")
    public void getType() {
        assertSame(validator.getType(), BaseType.get("TEXT"));
    }

    @Test
    @DisplayName("should be valid when input is null value")
    public void nullValue() {
        final List<ValidationResult> validationResult = validator.validate("TEST_FIELD_ID", null, caseField);
        assertThat(validationResult, hasSize(0));
    }

    @Test
    @DisplayName("should be valid when input is null text node")
    public void nullTextValue() {
        final List<ValidationResult> validationResult = validator.validate("TEST_FIELD_ID", new TextNode(null), caseField);
        assertThat(validationResult, hasSize(0));
    }
}
