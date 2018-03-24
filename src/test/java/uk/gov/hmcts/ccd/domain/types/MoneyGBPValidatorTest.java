package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
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
import static uk.gov.hmcts.ccd.domain.types.MoneyGBPValidator.TYPE_ID;

public class MoneyGBPValidatorTest extends BaseTest {
    private JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n" +
        "  \"id\": \"TEST_FIELD_ID\",\n" +
        "  \"field_type\": {\n" +
        "    \"type\": \"MoneyGBP\"\n" +
        "  }\n" +
        "}";

    @Inject
    private MoneyGBPValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        caseField = MAPPER.readValue(CASE_FIELD_STRING, CaseField.class);
    }

    @Test
    public void validMoney() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"177978989700\""), caseField);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"-100\""), caseField);
        assertEquals(0, result02.size());
    }

    @Test
    public void nullMoney() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", null, caseField);
        assertEquals("Did not catch null", 0, result01.size());
    }

    @Test
    public void invalidMoney() throws Exception {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"3321M1 1AA\""), caseField);
        assertEquals(result01.toString(), 1, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"100.1\""), caseField);
        assertEquals(result02.toString(), 1, result01.size());
    }

    @Test
    public void checkMaxMin_BothBelow1GBP() throws Exception {
        final String caseFiledString =
            "{\n" +
            "  \"id\": \"TEST_FIELD_ID\",\n" +
            "  \"field_type\": {\n" +
            "    \"type\": \"MONEY_GBP\",\n" +
            "    \"max\": 10,\n" +
            "    \"min\": 5\n" +
            "  }\n" +
            "}";

        final CaseField minMaxCaseField = MAPPER.readValue(caseFiledString, CaseField.class);

        // Test valid max min
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"5\""), minMaxCaseField);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"10\""), minMaxCaseField);
        assertEquals(0, result02.size());

        final List<ValidationResult> result03 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"7\""), minMaxCaseField);
        assertEquals(0, result03.size());

        // Test invalid max min
        final List<ValidationResult> result04 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"4\""), minMaxCaseField);
        assertEquals(1, result04.size());
        assertEquals("Should be more than or equal to £0.05", result04.get(0).getErrorMessage());

        final List<ValidationResult> result05 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"11\""), minMaxCaseField);
        assertEquals(1, result05.size());
        assertEquals("Should be less than or equal to £0.10", result05.get(0).getErrorMessage());
    }

    @Test
    public void checkMaxMin_BothAbove1GBP() throws Exception {
        final String caseFiledString =
            "{\n" +
                "  \"id\": \"TEST_FIELD_ID\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"MONEY_GBP\",\n" +
                "    \"max\": 123456,\n" +
                "    \"min\": 123\n" +
                "  }\n" +
                "}";

        final CaseField minMaxCaseField = MAPPER.readValue(caseFiledString, CaseField.class);

        // Test invalid max min
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"123457\""), minMaxCaseField);
        assertEquals(1, result01.size());
        assertEquals("Should be less than or equal to £1,234.56", result01.get(0).getErrorMessage());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", MAPPER.readTree("\"122\""), minMaxCaseField);
        assertEquals(1, result02.size());
        assertEquals("Should be more than or equal to £1.23", result02.get(0).getErrorMessage());
    }

    @Test
    public void shouldFail_whenDataValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not valid " + TYPE_ID));
    }

    @Test
    public void shouldFail_whenDataValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("Ngitb".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not valid " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenDataValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    public void shouldPass_whenDataValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode(1000), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1000 is not valid " + TYPE_ID));
    }

    @Test
    public void shouldPass_whenDataValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not valid " + TYPE_ID));
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get(TYPE_ID));
    }
}
