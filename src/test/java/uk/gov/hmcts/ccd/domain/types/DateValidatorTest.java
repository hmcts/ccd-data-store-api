package uk.gov.hmcts.ccd.domain.types;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

public class DateValidatorTest extends BaseTest implements IVallidatorTest {
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"DATE\"\n" +
        "  }\n" +
        "}";
    private final String LIMITED_REGEX = "^\\\\d{4}-\\\\d{2}-\\\\d{2}$";

    @Inject
    private DateValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
        ReflectionTestUtils.setField(validator.getType(), "regularExpression",
            "^(\\d{4})\\D?(0[1-9]|1[0-2])\\D?([12]\\d|0[1-9]|3[01])([zZ]|([\\+-])([01]\\d|2[0-3])\\D?([0-5]\\d)?)?$");
    }

    @Test
    public void validDate() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21"), caseField);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21Z"), caseField);
        assertEquals(0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2012-04-21+01:00"), caseField);
        assertEquals(0, result03.size());
    }

    @Test
    public void invalidDate() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("3321M1 1AA"), caseField);
        assertEquals("Did not catch invalid date 3321M1 1AA", 1, result01.size());
        assertEquals("\"3321M1 1AA\" is not a valid ISO 8601 date", result01.get(0).getErrorMessage());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("1800-14-14"), caseField);
        assertEquals("Did not catch invalid date 1800-14-14 ", 1, result02.size());
        assertEquals("\"1800-14-14\" is not a valid ISO 8601 date", result02.get(0).getErrorMessage());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-11-31"), caseField);
        assertEquals("Did not catch invalid date 2001-11-31", 1, result03.size());
        assertEquals("\"2001-11-31\" is not a valid ISO 8601 date", result03.get(0).getErrorMessage());

        // checks that ISO DATE TIME is not accepted by DateValidator
        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-01-01T00:00:00.000Z"), caseField);
        assertEquals("Did not catch invalid date 2001-01-01T00:00:00.000Z", 1, result04.size());
        assertEquals("\"2001-01-01T00:00:00.000Z\" is not a valid ISO 8601 date", result04.get(0).getErrorMessage());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2000-02-29Z"), caseField);
        assertEquals("Year 2000 is a leap year", 0, result05.size());

        final List<ValidationResult> result06 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2100-02-29Z"), caseField);
        assertEquals("Did not catch invalid date 2100-02-29Z", 1, result06.size());
        assertEquals("\"2100-02-29Z\" is not a valid ISO 8601 date", result06.get(0).getErrorMessage());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("DATE"));
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate("TEST_FIELD_ID", null, null).size());
    }

    @Test
    public void checkMax() throws Exception {
        final String validDate = "2001-01-01Z";
        final String invalidDate = "2002-01-01Z";
        final String maxDate = "2001-12-31+01:00";
        final Long maxDateTime = convertToTime(maxDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
            "  \"id\": \"DATE_TEST\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"DATE\",\n" +
            "    \"max\": \"" + maxDateTime + "\"\n" +
            "  }\n" +
            "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidDate), caseField);
        assertEquals("Did not catch invalid max-date", 1, result03.size());
        assertEquals("Validation message", "The date should be earlier than 31-12-2001",
            result03.get(0).getErrorMessage());
    }

    @Test
    public void checkMin() throws Exception {
        final String validDate = "2001-12-31Z";
        final String invalidDate = "2000-01-01Z";
        final String minDate = "2001-01-01Z";
        final Long minDateTime = convertToTime(minDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
            "  \"id\": \"DATE_TEST\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"DATE\",\n" +
            "    \"min\": \"" + minDateTime + "\"\n" +
            "  }\n" +
            "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidDate), caseField);
        assertEquals("Did not catch invalid max-date", 1, result03.size());
        assertEquals("Validation message", "The date should be later than 01-01-2001",
            result03.get(0).getErrorMessage());
    }

    @Test
    public void checkMaxMinWithoutRegEx() throws Exception {
        final String validDate = "2001-12-10Z";
        final String invalidMinDate = "1999-12-31Z";
        final String invalidMaxDate = "2002-01-01Z";
        final String minDate = "2001-01-01Z";
        final Long minDateTime = convertToTime(minDate);
        final String maxDate = "2001-12-31Z";
        final Long maxDateTime = convertToTime(maxDate);
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATE\",\n" +
                "    \"max\": \"" + maxDateTime + "\",\n" +
                "    \"min\": \"" + minDateTime + "\"\n" +
                "  }\n" +
                "}", CaseField.class);

        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(result01.toString(), 0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(result02.toString(), 0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(result03.toString(), 0, result03.size());

        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidMinDate), caseField);
        assertEquals("Did not catch invalid min-date", 1, result04.size());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(invalidMaxDate), caseField);
        assertEquals("Did not catch invalid max-date", 1, result05.size());
    }

    @Test
    public void invalidFieldTypeRegEx() throws Exception {
        final Long minDateTime = convertToTime("2001-01-01");
        final Long maxDateTime = convertToTime("2001-12-31");
        final String validDate = "2001-12-10Z";

        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATE\",\n" +
                "    \"max\": \"" + maxDateTime + "\",\n" +
                "    \"min\": \"" + minDateTime + "\",\n" +
                "    \"regular_expression\": \"" + "InvalidRegEx" + "\"\n" +
                "  }\n" +
                "}", CaseField.class);
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDate), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals(REGEX_GUIDANCE, result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

    @Test
    public void invalidBaseTypeRegEx() throws Exception {
        ReflectionTestUtils.setField(validator.getType(), "regularExpression", "InvalidRegEx");

        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATE\"\n" +
                "  }\n" +
                "}", CaseField.class);
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode("2001-12-10"), caseField);
        assertEquals("RegEx validation failed", 1, result.size());
        assertEquals(REGEX_GUIDANCE, result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }


    @Test
    public void validRegEx() throws Exception {
        final String validDate = "2001-12-10";
        final CaseField caseField = MAPPER.readValue(
            "{\n" +
                "  \"id\": \"DATE_TEST\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"DATE\",\n" +
                "    \"regular_expression\": \"" + LIMITED_REGEX + "\"\n" +
                "  }\n" +
                "}", CaseField.class);

        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
            NODE_FACTORY.textNode(validDate), caseField);
        assertEquals("RegEx validation failed", 0, result.size());
    }

    @Test
    public void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not a valid ISO 8601 date"));
    }

    @Test
    public void shouldFail_whenDataValueIsBinary() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("Ngitb".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not a valid ISO 8601 date"));
    }

    @Test
    public void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not a valid ISO 8601 date"));
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
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not a valid ISO 8601 date"));
    }

    private Long convertToTime(final String dateString) throws ParseException {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.parse(dateString).getTime();
    }
}
