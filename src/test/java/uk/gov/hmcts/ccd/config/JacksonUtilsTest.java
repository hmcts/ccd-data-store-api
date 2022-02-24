package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

class JacksonUtilsTest {

    @Test
    public void buildFromDottedPath() {
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
    public void shouldMergeIntoEmptyCaseData() throws Exception {
        Map<String, JsonNode> caseData = Maps.newHashMap();
        Map<String, JsonNode> defaultValueData = organisationPolicyDefaultValue("[Claimant]");

        JacksonUtils.merge(defaultValueData, caseData);

        assertEquals("Merged result does not match expected", defaultValueData, caseData);
    }

    @Test
    public void shouldMergeIntoExistingCaseData() throws Exception {
        Map<String, JsonNode> caseData = organisationPolicyCaseData("[Defendant]");
        Map<String, JsonNode> defaultValueData = organisationPolicyDefaultValue("[Claimant]");

        JacksonUtils.merge(defaultValueData, caseData);

        Map<String, JsonNode> expectedData = organisationPolicyCaseData("[Claimant]");
        assertEquals("Merged defaultValue does not match expected", expectedData, caseData);
    }

    @Test
    public void shouldOmitTopLevelCollectionOnMerge() throws Exception {
        Map<String, JsonNode> caseData = topLevelCollectionCaseData();
        Map<String, JsonNode> defaultValueData = topLevelCollectionDefaultValue();

        JacksonUtils.merge(defaultValueData, caseData);

        assertEquals("Merged defaultValue does not match expected", topLevelCollectionCaseData(), caseData);
    }

    @Test
    public void shouldOmitNestedCollectionOnMerge() throws Exception {

        Map<String, JsonNode> caseData = mySchoolDataWithNestedCollection("Test school name",
            "Class Name 1",
            "Class Name 2");
        Map<String, JsonNode> defaultValueData = mySchoolDefaultValueData("Updated name", "Updated Class Name");

        JacksonUtils.merge(defaultValueData, caseData);

        Map<String, JsonNode> expectedData = mySchoolDataWithNestedCollection("Updated name",
            "Class Name 1",
            "Class Name 2");
        assertEquals("Merged defaultValue does not match expected", expectedData, caseData);

    }

    @Test
    void testGetValueFromPath() throws JsonProcessingException {

        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
            "{"
                + "  \"Name\": \"NameValue\","
                + "  \"Class\": ["
                + "    {"
                + "      \"id\": \"6da7a0cf-8186-49d4-813d-c299d8f3491b\","
                + "      \"value\": {"
                + "        \"ClassName\": \"ClassNameValue1\""
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
                + "     {\"Number\" : 456},"
                + "     {\"Number\" : 789}"
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

        assertEquals("ClassNameValue1", JacksonUtils.getValueFromPath("Class.0.value.ClassName", data));
        assertEquals("ClassNameValue2", JacksonUtils.getValueFromPath("Class.1.value.ClassName", data));
        assertEquals("NameValue", JacksonUtils.getValueFromPath("Name", data));
        assertEquals("123", JacksonUtils.getValueFromPath("Number", data));
        assertEquals("456", JacksonUtils.getValueFromPath("NumberCollection.0.Number", data));
        assertEquals("789", JacksonUtils.getValueFromPath("NumberCollection.1.Number", data));
        assertEquals("Page Street 50", JacksonUtils.getValueFromPath("AddressCollection.0.value.AddressLine1", data));
        assertEquals("Address Line1", JacksonUtils.getValueFromPath("Person.Address.Line1", data));
        assertEquals("NameValue", JacksonUtils.getValueFromPath("Person.Name", data));
        assertEquals("http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",
            JacksonUtils.getValueFromPath("evidence.type.document_url", data));
        assertEquals(null,
            JacksonUtils.getValueFromPath("description", data));
        assertEquals("1637697929437509", JacksonUtils.getValueFromPath("CaseLinkCollection.0.CaseReference", data));
        assertEquals("1637697929619312", JacksonUtils.getValueFromPath("CaseLinkCollection.1.CaseReference", data));
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
