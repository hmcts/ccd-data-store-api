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
