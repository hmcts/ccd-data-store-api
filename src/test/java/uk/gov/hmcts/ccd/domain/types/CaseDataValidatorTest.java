package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CaseDataValidatorTest extends WireMockBaseTest {
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private static final String CASE_FIELD_JSON = "/tests/CaseDataValidator_CaseField.json";
    private static String CASE_FIELDS_STRING;

    @Inject
    private CaseDataValidator caseDataValidator;
    private List<CaseFieldDefinition> caseFields;

    @Mock
    private TextCaseReferenceCaseLinkValidator textCaseReferenceCaseLinkValidator;


    @BeforeClass
    public static void setUpClass() {
        CASE_FIELDS_STRING = BaseTest.getResourceAsString(CASE_FIELD_JSON);
    }

    @Before
    public void setUp() throws IOException {
        caseFields = BaseTest.getCaseFieldsFromJson(CASE_FIELDS_STRING);
    }

    @Test
    public void validValueComplex() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Person\" : {\n" +
                "    \"Name\" : \"Name\",\n" +
                "    \"Address\": {\n" +
                "      \"Line1\": \"Address Line 1\",\n" +
                "      \"Line2\": \"Address Line 2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });

        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 0, result.size());
    }

    private ValidationContext getValidationContext(Map<String, JsonNode> values) {
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        return new ValidationContext(caseTypeDefinition, values);
    }


    @Test
    public void unrecognisedValueComplex() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Person\" : {\n" +
                "    \"Name\" : \"Name\",\n" +
                "    \"Address\": {\n" +
                "      \"Line1\": \"Address Line 1\",\n" +
                "      \"\": \"Address Line 2\",\n" +
                "      \"Line4\": \"Address Line 2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 2, result.size());
    }

    @Test
    public void shouldPrefixComplexChildrenIDWithPath() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Person\" : {\n" +
                "    \"Address\": {\n" +
                "      \"Line4\": \"Address Line 2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 1, result.size());

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId(), equalTo("Person.Address.Line4"));
    }

    @Test
    public void shouldPrefixComplexChildrenIDWithPath_oneLevel() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Person\" : {\n" +
                "    \"FirstName\": \"Invalid field\"," +
                "    \"Address\": {\n" +
                "      \"Line1\": \"Address Line 1\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 1, result.size());

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId(), equalTo("Person.FirstName"));
    }

    @Test
    public void shouldNotPrefixRootFields() throws Exception {

        final String DATA =
            "{\n" +
                "  \"PersonGender\" : \"\"\n" +
                "}";
        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 1, result.size());

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId(), equalTo("PersonGender"));
    }

    @Test
    public void unknownType() throws Exception {
        final String unknownType =
            "[\n" +
                "  {\n" +
                "    \"id\": \"Person\",\n" +
                "    \"field_type\": {\n" +
                "      \"type\": \"Unknown\"\n" +
                "    }\n" +
                "  }\n" +
                "]\n";
        final List<CaseFieldDefinition> caseFields = BaseTest.getCaseFieldsFromJson(unknownType);
        final String data =
            "{\n" +
                "  \"Person\" : {\n" +
                "    \"Name\" : \"Name\",\n" +
                "    \"Address\": {\n" +
                "      \"Line1\": \"Address Line 1\",\n" +
                "      \"\": \"Address Line 2\",\n" +
                "      \"Line4\": \"Address Line 2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final Map<String, JsonNode> values = MAPPER.readValue(data, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 2, result.size());
    }

    @Test
    public void validValueCollection() throws Exception {

        final String DATA =
            "{\n" +
                "  \"OtherAddresses\" : [\n" +
                "    {\n" +
                "      \"value\": \n" +
                "        {\n" +
                "          \"Line1\": \"Address Line 1\",\n" +
                "          \"Line2\": \"Address Line 2\"\n" +
                "        }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertEquals(result.toString(), 0, result.size());
    }

    @Test
    public void unknownFieldInCollectionOfComplex() throws Exception {

        final String DATA =
            "{\n" +
                "  \"OtherAddresses\" : [\n" +
                "    {\n" +
                "      \"value\": " +
                "        {\n" +
                "          \"UnknownField\": \"Address Line 1\",\n" +
                "          \"Line2\": \"Address Line 2\"\n" +
                "        }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 1, results.size());

        final ValidationResult result = results.get(0);
        assertThat(result.getFieldId(), equalTo("OtherAddresses.0.UnknownField"));
    }

    @Test
    public void exceedMaxItemsInCollection() throws Exception {

        final String DATA =
            "{\n" +
                "  \"OtherAddresses\" : [\n" +
                "    {\n" +
                "      \"value\": " +
                "        {\n" +
                "          \"Line1\": \"Address 1 Line 1\",\n" +
                "          \"Line2\": \"Address 1 Line 2\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "      \"value\": " +
                "        {\n" +
                "          \"Line1\": \"Address 2 Line 1\",\n" +
                "          \"Line2\": \"Address 2 Line 2\"\n" +
                "        }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 1, results.size());

        final ValidationResult result = results.get(0);
        assertThat(result.getFieldId(), equalTo("OtherAddresses"));
    }

    @Test
    public void validCollectionOfSimpleFields() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Initials\" : [ { \"value\": \"A\" }, { \"value\": \"B\" } ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 0, results.size());
    }

    @Test
    public void invalidCollectionOfSimpleFields() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Initials\" : [ { \"value\": \"TooLong\" }, { \"value\": \"B\" } ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 1, results.size());

        final ValidationResult result = results.get(0);
        assertThat(result.getFieldId(), equalTo("Initials.0"));
    }

    @Test
    public void multipleInvalidItemsInCollection() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Initials\" : [ { \"value\": \"TooLong\" }, { \"value\": \"TooLong\" } ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 2, results.size());

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId(), equalTo("Initials.0"));

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId(), equalTo("Initials.1"));
    }

    @Test
    public void invalidCollection_valuesMissing() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Initials\" : [ { \"x\": \"TooLong\" }, { \"y\": \"TooLong\" } ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 2, results.size());

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId(), equalTo("Initials.0"));

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId(), equalTo("Initials.1"));
    }

    @Test
    public void invalidCollection_notObject() throws Exception {

        final String DATA =
            "{\n" +
                "  \"Initials\" : [ \"x\", \"y\" ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 2, results.size());

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId(), equalTo("Initials.0"));

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId(), equalTo("Initials.1"));
    }

    @Test
    public void shouldFailForPredefinedType() throws Exception {
        final String DATA = "{\n" +
            "        \"CaseReference\": \"1596XXXXX1048-4059XXXOOOO\"\n" +
            "      }";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertEquals(results.toString(), 1, results.size());

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId(), equalTo("CaseReference"));
        assertThat(result0.getErrorMessage(),
            equalTo("The data entered is not valid for this type of field, please delete and re-enter using only valid"
                + " data")
        );
    }

    @Test
    public void shouldInvokeForPredefinedTypea() throws Exception {
        List<FieldValidator> fieldValidators = new ArrayList<>();
        fieldValidators.add(textCaseReferenceCaseLinkValidator);
        CaseDataValidator caseDataValidator = new CaseDataValidator(fieldValidators);

        when(textCaseReferenceCaseLinkValidator.getCustomTypeId()).thenReturn("TextCaseReference");

        final String DATA = "{\n" +
            "        \"CaseReference\": \"1596104840593131\"\n" +
            "      }";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);

        assertEquals(results.toString(), 0, results.size());
        verify(textCaseReferenceCaseLinkValidator).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldInvokeForPredefinedTypea2() throws Exception {
        List<FieldValidator> fieldValidators = new ArrayList<>();
        fieldValidators.add(textCaseReferenceCaseLinkValidator);
        fieldValidators.add(new CollectionValidator());
        CaseDataValidator caseDataValidator = new CaseDataValidator(fieldValidators);

        when(textCaseReferenceCaseLinkValidator.getCustomTypeId()).thenReturn("TextCaseReference");

        final String DATA =
            "{\n" +
                "  \"CaseLink\" : [\n" +
                "    {\n" +
                "      \"value\": " +
                "        {\n" +
                "          \"CaseLink1\": \"1596104840593131\",\n" +
                "          \"CaseLink2\": \"1596104840593131\"\n" +
                "        }\n" +
                "    }" +
                "  ]\n" +
                "}";

        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);

        assertEquals(results.toString(), 0, results.size());
        verify(textCaseReferenceCaseLinkValidator, times(2))
            .validate(any(ValidationContext.class));
    }

    @Test
    public void textFieldWithMaxMin() throws Exception {
        final String caseFieldString =
            "[{\n" +
                "  \"id\": \"PersonFirstName\",\n" +
                "  \"case_type_id\": \"TestAddressBookCase\",\n" +
                "  \"label\": \"First name\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Text\",\n" +
                "    \"max\": 100,\n" +
                "    \"min\": 10\n" +
                "  }\n" +
                "}]";
        final List<CaseFieldDefinition> caseFields =
            MAPPER.readValue(caseFieldString, TypeFactory.defaultInstance().constructCollectionType(List.class,
                CaseFieldDefinition.class));
        final String DATA = "{\"PersonFirstName\" : \"Test Name Test Name\"}";
        final Map<String, JsonNode> values = MAPPER.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        final ValidationContext validationContext = new ValidationContext(caseTypeDefinition, values);
        assertEquals(0, caseDataValidator.validate(validationContext).size());
    }

    /**
     * This test is only meant to ensure that validators are invoked and not to test the TextValidator which has
     * it own test.
     */
    @Test
    public void textFieldWithInvalidMaxMin() throws Exception {
        final String caseFieldString =
            "[{\n" +
                "  \"id\": \"PersonFirstName\",\n" +
                "  \"case_type_id\": \"TestAddressBookCase\",\n" +
                "  \"label\": \"First name\",\n" +
                "  \"field_type\": {\n" +
                "    \"type\": \"Text\",\n" +
                "    \"max\": 10,\n" +
                "    \"min\": 5\n" +
                "  }\n" +
                "}]";
        final List<CaseFieldDefinition> caseFields =
            MAPPER.readValue(caseFieldString, TypeFactory.defaultInstance().constructCollectionType(List.class,
                CaseFieldDefinition.class));

        final Map<String, JsonNode> invalidMaxVal =
            MAPPER.readValue("{\"PersonFirstName\" : \"Test Name Test Name\"}",
                new TypeReference<HashMap<String, JsonNode>>() {
                });

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        final ValidationContext validationContext = new ValidationContext(caseTypeDefinition, invalidMaxVal);
        assertEquals("Did not catch invalid max", 1, caseDataValidator.validate(validationContext).size());

        final Map<String, JsonNode> invalidMinVal =
            MAPPER.readValue("{\"PersonFirstName\" : \"Test\"}", new TypeReference<HashMap<String, JsonNode>>() {
            });
        final ValidationContext validationContext1 = new ValidationContext(caseTypeDefinition, invalidMinVal);
        assertEquals("Did not catch invalid max", 1, caseDataValidator.validate(validationContext1).size());
    }
}
