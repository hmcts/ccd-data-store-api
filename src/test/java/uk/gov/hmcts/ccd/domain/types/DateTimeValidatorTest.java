package uk.gov.hmcts.ccd.domain.types;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DateTimeValidatorTest extends BaseTest implements IVallidatorTest {
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"DATETIME\"\n" +
        "  }\n" +
        "}";

    @Inject
    private DateTimeValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        ReflectionTestUtils.setField(validator.getType(), "regularExpression",
            "^(\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|" +
                "(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)" +
                "([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$");
    }

    @Test
    public void validDate() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21T00:00:00.000"), caseField);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21T00:00:00.000Z"), caseField);
        assertEquals(0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21T00:00:00+01:00"), caseField);
        assertEquals(0, result03.size());
    }

    @Test
    public void invalidDate() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("3321M1 1AA"), caseField);
        assertEquals("Did not catch invalid date 3321M1 1AA", 1, result01.size());
        assertEquals("\"3321M1 1AA\" is not a valid ISO 8601 date time", result01.get(0).getErrorMessage());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("1800-14-14T00:00:00"), caseField);
        assertEquals("Did not catch invalid date 1800-14-14 ", 1, result02.size());
        assertEquals("\"1800-14-14T00:00:00\" is not a valid ISO 8601 date time", result02.get(0).getErrorMessage());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-11-31T00:00:00"), caseField);
        assertEquals("Did not catch invalid date time 2001-11-31", 1, result03.size());
        assertEquals("\"2001-11-31T00:00:00\" is not a valid ISO 8601 date time", result03.get(0).getErrorMessage());

        // checks that ISO DATE is not accepted by DateTimeValidator
        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-01-01"), caseField);
        assertEquals("Did not catch invalid date time 2001-01-01", 1, result04.size());
        assertEquals("\"2001-01-01\" is not a valid ISO 8601 date time", result04.get(0).getErrorMessage());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2000-02-29T00:00:00Z"), caseField);
        assertEquals("Year 2000 is a leap year", 0, result05.size());

        final List<ValidationResult> result06 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2100-02-29T00:00:00Z"), caseField);
        assertEquals("Did not catch invalid date 2100-02-29Z", 1, result06.size());
        assertEquals("\"2100-02-29T00:00:00Z\" is not a valid ISO 8601 date time", result06.get(0).getErrorMessage());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("DATETIME"));
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, null).size());
    }

    @Test
    public void checkMax() throws Exception {
        final String validDateTime = "2001-01-01T00:00:00Z";
        final String invalidDateTime = "2002-01-01T00:00:00Z";
        final String maxDate = "2001-12-31T00:00:00+01:00";
        final Long maxDateTime = convertToLongTime(maxDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
            "  \"id\": \"DATE_TEST\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"DATETIME\",\n" +
            "    \"max\": \"" + maxDateTime + "\"\n" +
            "  }\n" +
            "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDateTime), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidDateTime), caseField);
        assertEquals("Did not catch invalid max-date", 1, result03.size());
        assertEquals("Validation message", "The date time should be earlier than 2001-12-31T00:00:00",
            result03.get(0).getErrorMessage());
    }

    @Test
    public void checkMin() throws Exception {
        final String validDateTime = "2001-12-31T00:00:00Z";
        final String invalidDateTime = "2000-01-01T00:00:00Z";
        final String minDate = "2001-01-01T00:00:00Z";
        final Long minDateTime = convertToLongTime(minDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
            "  \"id\": \"DATE_TEST\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"DATETIME\",\n" +
            "    \"min\": \"" + minDateTime + "\"\n" +
            "  }\n" +
            "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDateTime), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidDateTime), caseField);
        assertEquals("Did not catch invalid max-date", 1, result03.size());
        assertEquals("Validation message", "The date time should be later than 2001-01-01T00:00:00",
            result03.get(0).getErrorMessage());
    }

    @Test
    public void checkMaxMinWithoutRegEx() throws Exception {
        final String validDateTime = "2001-12-10T00:00:00Z";
        final String invalidMinDateTime = "1999-12-31T00:00:00Z";
        final String invalidMaxDateTime = "2002-01-01T00:00:00Z";
        final String minDate = "2001-01-01T00:00:00Z";
        final Long minDateTime = convertToLongTime(minDate);
        final String maxDate = "2001-12-31T00:00:00Z";
        final Long maxDateTime = convertToLongTime(maxDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATETIME\",\n" +
                "    \"max\": \"" + maxDateTime + "\",\n" +
                "    \"min\": \"" + minDateTime + "\"\n" +
                "  }\n" +
                "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDateTime), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(result03.toString(), 0, result03.size());

        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidMinDateTime), caseField);
        assertEquals("Did not catch invalid min-date", 1, result04.size());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidMaxDateTime), caseField);
        assertEquals("Did not catch invalid max-date", 1, result05.size());
    }

    @Test
    public void invalidFieldTypeRegEx() throws Exception {
        final Long minDateTime = convertToLongTime("2001-01-01T00:00:00");
        final Long maxDateTime = convertToLongTime("2001-12-31T00:00:00");
        final String validDateTime = "2001-12-10T00:00:00Z";

        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATETIME\",\n" +
                "    \"max\": \"" + maxDateTime + "\",\n" +
                "    \"min\": \"" + minDateTime + "\",\n" +
                "    \"regular_expression\": \"" + "InvalidRegEx" + "\"\n" +
                "  }\n" +
                "}", CaseField.class);
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDateTime), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals("2001-12-10T00:00:00Z Field Type Regex Failed:InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

    @Test
    public void invalidBaseTypeRegEx() throws Exception {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "InvalidRegEx");

        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATETIME\"\n" +
                "  }\n" +
                "}", CaseField.class);
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-12-10T00:00:00"), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals("2001-12-10T00:00:00 Date Time Type Regex Failed:InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }


    @Test
    public void validRegEx() throws Exception {
        final String validDateTime = "2001-12-10T00:00:00";
        String LIMITED_REGEX = "^\\\\d{4}-\\\\d{2}-\\\\d{2}[T\\\\s]\\\\d{2}:\\\\d{2}:\\\\d{2}$";
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATETIME\",\n" +
                "    \"regular_expression\": \"" + LIMITED_REGEX + "\"\n" +
                "  }\n" +
                "}", CaseField.class);

        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDateTime), caseField);
        assertEquals("RegEx validation failed", 0, result.size());
    }

    @Test
    public void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not a valid ISO 8601 date time"));
    }

    @Test
    public void shouldFail_whenDataValueIsBinary() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("Ngitb".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not a valid ISO 8601 date time"));
    }

    @Test
    public void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not a valid ISO 8601 date time"));
    }

    @Test
    public void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result = validator.validate("TEST_FIELD_ID", NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldFail_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not a valid ISO 8601 date time"));
    }

    private Long convertToLongTime(final String dateString) {
        final LocalDateTime dateTimeValue = LocalDateTime.parse(dateString, ISO_DATE_TIME);
        return dateTimeValue.toEpochSecond(ZoneOffset.UTC) * 1000;
    }
}
