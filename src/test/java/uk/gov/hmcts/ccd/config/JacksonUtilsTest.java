package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

class JacksonUtilsTest {
    private static final String NULL_MARKER = "NULL";
    private static final String UUID_MARKER = "UUID";

    @Test
    void buildFromDottedPath() {
        JsonNode result = JacksonUtils.buildFromDottedPath("FieldA.FieldB.FieldC", "Test value");

        assertThat(result.getNodeType(), is(JsonNodeType.OBJECT));
        assertThat(result.size(), is(1));
        assertNotNull(result.get("FieldA"));
        assertNotNull(result.get("FieldA").get("FieldB"));
        assertNotNull(result.get("FieldA").get("FieldB").get("FieldC"));
        assertThat(result.get("FieldA").get("FieldB").get("FieldC").getNodeType(), is(JsonNodeType.STRING));
        assertThat(result.get("FieldA").get("FieldB").get("FieldC").asText(), is("Test value"));
    }

    @Test
    void shouldMergeIntoEmptyCaseData() throws Exception {
        Map<String, JsonNode> caseData = Maps.newHashMap();
        Map<String, JsonNode> defaultValueData = organisationPolicyDefaultValue("[Claimant]");

        JacksonUtils.merge(defaultValueData, caseData);

        assertEquals(defaultValueData, caseData, "Merged result does not match expected");
    }

    @Test
    void shouldMergeIntoExistingCaseData() throws Exception {
        Map<String, JsonNode> caseData = organisationPolicyCaseData("[Defendant]");
        Map<String, JsonNode> defaultValueData = organisationPolicyDefaultValue("[Claimant]");

        JacksonUtils.merge(defaultValueData, caseData);

        Map<String, JsonNode> expectedData = organisationPolicyCaseData("[Claimant]");
        assertEquals(expectedData, caseData, "Merged defaultValue does not match expected");
    }

    @Test
    void shouldOmitTopLevelCollectionOnMerge() throws Exception {
        Map<String, JsonNode> caseData = topLevelCollectionCaseData();
        Map<String, JsonNode> defaultValueData = topLevelCollectionDefaultValue();

        JacksonUtils.merge(defaultValueData, caseData);

        assertEquals(topLevelCollectionCaseData(), caseData, "Merged defaultValue does not match expected");
    }

    @Test
    void shouldOmitNestedCollectionOnMerge() throws Exception {

        Map<String, JsonNode> caseData = mySchoolDataWithNestedCollection("Test school name",
            "Class Name 1",
            "Class Name 2");
        Map<String, JsonNode> defaultValueData = mySchoolDefaultValueData("Updated name", "Updated Class Name");

        JacksonUtils.merge(defaultValueData, caseData);

        Map<String, JsonNode> expectedData = mySchoolDataWithNestedCollection("Updated name",
            "Class Name 1",
            "Class Name 2");
        assertEquals(expectedData, caseData, "Merged defaultValue does not match expected");

    }

    @Test
    void shouldMergeNullTopLevelValue() {
        Map<String, JsonNode> mergeFrom = new HashMap<>();
        mergeFrom.put("TextField0", MAPPER.getNodeFactory().textNode("Default text"));
        Map<String, JsonNode> mergeInto = new HashMap<>();
        mergeInto.put("TextField0", MAPPER.getNodeFactory().nullNode());

        JacksonUtils.merge(mergeFrom, mergeInto);

        assertEquals("Default text", mergeInto.get("TextField0").asText());
    }

    @Test
    void shouldNotUseDefaultValueWhenTopLevelValueExists() {
        Map<String, JsonNode> mergeFrom = new HashMap<>();
        mergeFrom.put("TextField0", MAPPER.getNodeFactory().textNode("Default text"));
        Map<String, JsonNode> mergeInto = new HashMap<>();
        mergeInto.put("TextField0", MAPPER.getNodeFactory().textNode("Existing text"));

        JacksonUtils.merge(mergeFrom, mergeInto);

        assertEquals("Existing text", mergeInto.get("TextField0").asText());
    }

    @Test
    void shouldOnlyUseDefaultValueWhenTopLevelKeyNotPresent() {
        Map<String, JsonNode> mergeFrom = new HashMap<>();
        mergeFrom.put("TextField0", MAPPER.getNodeFactory().textNode("Default text"));
        Map<String, JsonNode> mergeInto = new HashMap<>();
        mergeInto.put("TextField1", MAPPER.getNodeFactory().textNode("Existing text"));

        JacksonUtils.merge(mergeFrom, mergeInto);

        assertEquals("Default text", mergeInto.get("TextField0").asText());
        assertEquals("Existing text", mergeInto.get("TextField1").asText());
    }

    @ParameterizedTest(name = "Test getValueFromPath - #{index} - `{0}`")
    @CsvSource({
        "String," + "StringValue",

        "StringCollection.0," + "String1",
        "StringCollection.0.value," + "String1",
        "StringCollection.1," + "String2",
        "StringCollection.1.value," + "String2",

        "StringNotACollection.0," + "String3",
        "StringNotACollection.1," + "String4",

        "Class.0.value.ClassName," + "ClassNameValue1",
        "Class.0.ClassName," + "ClassNameValue1",
        "Class.0.NestedCollection," + NULL_MARKER,
        "Class.0.NestedCollection.0.property1," + "NestedColProperty1",
        "Class.0.NestedCollection.0.id," + UUID_MARKER,
        "Class.0.NestedComplex.property1," + "NestedComplexProperty1",

        "Class.1.value.ClassName," + "ClassNameValue2",
        "Class.1.ClassName," + "ClassNameValue2",

        "Class," + NULL_MARKER, // no collection index/property to load
        "Class.0.NestedCollection," + NULL_MARKER, // no collection index/property to load
        "ClassNotFound," + NULL_MARKER,
        "ClassNotFound.1.Property," + NULL_MARKER,
        "Class.1.NotFound," + NULL_MARKER,
        "Class.2.BadIndex," + NULL_MARKER,
        "Class.NotAnIndex," + NULL_MARKER,

        "Number," + "123",

        "NumberCollection.0," + "456",
        "NumberCollection.1," + "789",

        "NumberNotACollection.0," + "111",
        "NumberNotACollection.1," + "222",

        "ArrayWithNumbers.0.Number," + "333",
        "ArrayWithNumbers.1.Number," + "444",

        "AddressCollection.0.value.AddressLine1," + "Page Street 50",

        "Person.Address.Line1," + "Address Line1",
        "Person.Name," + "NameValue",

        "evidence.type.document_url," + "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",

        "description," + NULL_MARKER,

        "CaseLinkCollection.0.CaseReference," + "1637697929437509",
        "CaseLinkCollection.1.CaseReference," + "1637697929619312"
    })
    void testGetValueFromPath(final String path, final String expected) throws JsonProcessingException {

        // ARRANGE
        final String testId = UUID.randomUUID().toString();
        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
            "{"
                + "  \"String\": \"StringValue\","
                + "  \"StringCollection\": ["
                + "    {"
                + "      \"id\": \"dbb19fb5-6647-4d6f-9bbb-f0b9fe3f3b49\","
                + "      \"value\": \"String1\""
                + "    },"
                + "    {"
                + "      \"id\": \"ea73d66f-9388-4344-afba-eac5d37b0baf\","
                + "      \"value\": \"String2\""
                + "    }"
                + "  ],"
                + "  \"StringNotACollection\": ["
                + "    \"String3\","
                + "    \"String4\""
                + "   ],"
                + "  \"Class\": ["
                + "    {"
                + "      \"id\": \"6da7a0cf-8186-49d4-813d-c299d8f3491b\","
                + "      \"value\": {"
                + "        \"ClassName\": \"ClassNameValue1\","
                + "        \"NestedCollection\": ["
                + "          {"
                + "            \"id\": \"" + testId + "\","
                + "            \"value\": {"
                + "              \"property1\" : \"NestedColProperty1\""
                + "             }"
                + "          }"
                + "        ],"
                + "        \"NestedComplex\": {"
                + "          \"property1\" : \"NestedComplexProperty1\""
                + "        }"
                + "      }"
                + "    },"
                + "    {"
                + "      \"id\": \"b7662626-b640-48bb-8afa-9fa78dcbd2ec\","
                + "      \"value\": {"
                + "        \"ClassName\": \"ClassNameValue2\""
                + "      }"
                + "    }"
                + "  ],"
                + "  \"Number\": 123,"
                + "  \"NumberCollection\": ["
                + "    {"
                + "      \"id\": \"7416b8e4-4912-4b9e-b888-61bba42079c2\","
                + "      \"value\": 456"
                + "    },"
                + "    {"
                + "      \"id\": \"1b4abdf3-0ea7-4dcb-ab6a-c85975a43ab3\","
                + "      \"value\": 789"
                + "    }"
                + "  ],"
                + "  \"NumberNotACollection\": ["
                + "     111,"
                + "     222"
                + "  ],"
                + "  \"ArrayWithNumbers\": ["
                + "     {\"Number\": 333},"
                + "     {\"Number\": 444}"
                + "  ],"
                + "  \"AddressCollection\": ["
                + "    {"
                + "        \"id\": null,"
                + "        \"value\": {"
                + "            \"AddressLine1\": \"Page Street 50\""
                + "        }"
                + "    },"
                + "    {"
                + "        \"id\": null,"
                + "        \"value\": {"
                + "            \"AddressLine1\": \"Blueberry Rd 8\""
                + "        }"
                + "    }"
                + "   ],"
                + "   \"Person\": {\n"
                + "        \"Name\": \"NameValue\",\n"
                + "        \"Address\": {\n"
                + "            \"Line1\": \"Address Line1\"\n,"
                + "            \"Line2\": \"Address Line1\"\n"
                + "         }\n"
                + "  },\n"
                + "  \"evidence\": {"
                + "     \"type\": {"
                + "             \"document_url\": \"http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea\","
                + "             \"document_filename\": \"B_Document Inside Complex Type.docx\","
                + "             \"document_binary_url\": \"http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea/binary\""
                + "     },"
                + "     \"description\": null"
                + "  },"
                + " \"CaseLinkCollection\" : [ {\n"
                + "        \"value\" : {\n"
                + "          \"CaseReference\" : \"1637697929437509\"\n"
                + "        },\n"
                + "        \"id\" : \"90a2df83-f256-43ec-aaa0-48e127a44402\"\n"
                + "      }, {\n"
                + "        \"value\" : {\n"
                + "          \"CaseReference\" : \"1637697929619312\"\n"
                + "        },\n"
                + "        \"id\" : \"84e22baf-5bec-4eec-a31f-7a3954efc9c3\"\n"
                + "      } ]"
                + "}"));

        // ACT
        String result = JacksonUtils.getValueFromPath(path, data);

        // ASSERT
        if (NULL_MARKER.equals(expected)) {
            assertNull(result);
        } else if (UUID_MARKER.equals(expected)) {
            assertEquals(testId, result);
        } else {
            assertEquals(expected, result);
        }
    }

    static Map<String, JsonNode> mySchoolDataWithNestedCollection(String name, String className1, String className2)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"Name\": \"" + name + "\","
            + "  \"Class\": ["
            + "    {"
            + "      \"id\": \"6da7a0cf-8186-49d4-813d-c299d8f3491b\","
            + "      \"value\": {"
            + "        \"ClassName\": \"" + className1 + "\""
            + "      }"
            + "    },"
            + "    {"
            + "      \"id\": \"b7662626-b640-48bb-8afa-9fa78dcbd2ec\","
            + "      \"value\": {"
            + "        \"ClassName\": \"" + className2 + "\""
            + "      }"
            + "    }"
            + "  ],"
            + "  \"Number\": null"
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("MySchool", data);
        return result;
    }

    static Map<String, JsonNode> mySchoolDefaultValueData(String name, String className)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "    \"Name\": \"" + name + "\","
            + "    \"Class\": {"
            + "        \"ClassName\": \"" + className + "\""
            + "    }"
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("MySchool", data);
        return result;
    }

    static Map<String, JsonNode> topLevelCollectionCaseData()
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "["
            + "    {"
            + "        \"id\": null,"
            + "        \"value\": {"
            + "            \"AddressLine1\": \"Page Street 50\""
            + "        }"
            + "    },"
            + "    {"
            + "        \"id\": null,"
            + "        \"value\": {"
            + "            \"AddressLine1\": \"Blueberry Rd 8\""
            + "        }"
            + "    }"
            + "]");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("CollectionComplexField", data);
        return result;
    }

    static Map<String, JsonNode> topLevelCollectionDefaultValue()
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"AddressLine1\": \"Updated Address\""
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("CollectionComplexField", data);
        return result;
    }

    static Map<String, JsonNode> organisationPolicyCaseData(String role)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"Organisation\": {"
            + "    \"OrganisationID\": null,"
            + "    \"OrganisationName\": null"
            + "  },"
            + "  \"OrgPolicyReference\": null,"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\""
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("OrganisationPolicyField", data);
        return result;
    }

    static Map<String, JsonNode> organisationPolicyDefaultValue(String role)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\""
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("OrganisationPolicyField", data);
        return result;
    }
}
