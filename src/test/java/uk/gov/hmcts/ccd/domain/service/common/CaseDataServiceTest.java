package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.caseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataClassificationBuilder.dataClassification;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseDataServiceTest {
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CaseDataService caseDataService = new CaseDataService();
    private CaseType caseType;

    @BeforeEach
    public void setUp() {
        setCaseType(RESTRICTED);
    }

    private void setCaseType(SecurityClassification securityClassification) {
        final FieldType textFieldType = aFieldType().withType("Text")
                                           .build();

        CaseField postalAddress = aCaseField()
            .withId("PostalAddress")
            .withSC(SecurityClassification.PRIVATE.name())
            .withFieldType(aFieldType()
                               .withType("Complex")
                               .withComplexField(aCaseField()
                                                     .withId("AddressLine1")
                                                     .withFieldType(textFieldType)
                                                     .withSC(securityClassification.name())
                                                     .build())
                               .withComplexField(aCaseField()
                                                     .withId("AddressLine2")
                                                     .withFieldType(textFieldType)
                                                     .withSC(securityClassification.name())
                                                     .build())
                               .withComplexField(aCaseField()
                                                     .withId("AddressLine3")
                                                     .withFieldType(textFieldType)
                                                     .withSC(securityClassification.name())
                                                     .build())
                               .withComplexField(aCaseField()
                                                     .withId("Country")
                                                     .withFieldType(textFieldType)
                                                     .withSC(PRIVATE.name())
                                                     .build())
                               .withComplexField(aCaseField()
                                                     .withId("PostCode")
                                                     .withFieldType(textFieldType)
                                                     .withSC(RESTRICTED.name())
                                                     .build())
                               .withComplexField(aCaseField()
                                                     .withId("Occupant")
                                                     .withFieldType(aFieldType()
                                                                        .withType("Complex")
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "Title")
                                                                                              .withFieldType(
                                                                                                  textFieldType)
                                                                                              .withSC(
                                                                                                  PUBLIC.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "FirstName")
                                                                                              .withFieldType(
                                                                                                  textFieldType)
                                                                                              .withSC(
                                                                                                  PUBLIC.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "MiddleName")
                                                                                              .withFieldType(
                                                                                                  textFieldType)
                                                                                              .withSC(
                                                                                                  PRIVATE.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "LastName")
                                                                                              .withFieldType(
                                                                                                  textFieldType)
                                                                                              .withSC(
                                                                                                  PRIVATE.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "DateOfBirth")
                                                                                              .withFieldType(
                                                                                                  aFieldType()
                                                                                                      .withType(
                                                                                                          "Date")
                                                                                                      .build())
                                                                                              .withSC(
                                                                                                  PRIVATE.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "NationalInsuranceNumber")
                                                                                              .withFieldType(
                                                                                                  textFieldType)
                                                                                              .withSC(
                                                                                                  RESTRICTED.name())
                                                                                              .build())
                                                                        .withComplexField(aCaseField()
                                                                                              .withId(
                                                                                                  "MaritalStatus")
                                                                                              .withFieldType(
                                                                                                  aFieldType()
                                                                                                      .withType(
                                                                                                          "FixedList")
                                                                                                      .build())
                                                                                              .withSC(
                                                                                                  RESTRICTED.name())
                                                                                              .build())
                                                                        .build())
                                                     .withSC(PUBLIC.name())
                                                     .build())
                               .build())
            .build();
        caseType = TestBuildersUtil.CaseTypeBuilder.aCaseType()
            .withField(aCaseField()
                           .withId("ClientsAddresses")
                           .withSC(PRIVATE.name())
                           .withFieldType(aFieldType()
                                              .withType("Collection")
                                              .withCollectionFieldType(aFieldType()
                                                                           .withId("Address")
                                                                           .withType("Complex")
                                                                           .withComplexField(postalAddress)
                                                                           .build()
                                              )
                                              .build())
                           .build()
            )
            .withField(aCaseField()
                           .withId("Company")
                           .withSC(PUBLIC.name())
                           .withFieldType(
                               aFieldType()
                                   .withType("Complex")
                                   .withComplexField(aCaseField()
                                                         .withId("Name")
                                                         .withFieldType(textFieldType)
                                                         .withSC(PRIVATE.name())
                                                         .build())
                                   .withComplexField(postalAddress)
                                   .build()
                           )
                           .build()
            )
            .withField(aCaseField()
                           .withId("OtherInfo")
                           .withFieldType(textFieldType)
                           .withSC(PRIVATE.name())
                           .build())
            .withField(aCaseField().withId("simple_collection")
                                   .withSC("PUBLIC")
                                   .withFieldType(aFieldType().withType("Collection")
                                                              .withCollectionFieldType(textFieldType)
                                                              .build()
                                   )
                                   .build()
            )
            .build();
    }

    @Test
    @DisplayName("should get the default security classifications")
    void shouldGetDefaultClassifications() throws IOException, JSONException {
        final Map<String, JsonNode> DATA = MAPPER.convertValue(MAPPER.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"Company\": {\n" +
                "    \"PostalAddress\": {\n" +
                "      \"AddressLine1\": \"Address Line 1\",\n" +
                "      \"AddressLine2\": \"Address Line 2\",\n" +
                "      \"AddressLine3\": \"Address Line 3\",\n" +
                "      \"Occupant\": {\n" +
                "           \"Title\": \"Mr\"," +
                "           \"FirstName\": \"John\"," +
                "           \"MiddleName\": \"Arthur\"," +
                "           \"LastName\": \"Smith\"," +
                "           \"DateOfBirth\": \"12-12-1992\"," +
                "           \"NationalInsuranceNumber\": \"SK123456D\"," +
                "           \"MaritalStatus\": \"Married\"" +
                "      }\n" +
                "    }\n" +
                "    },\n" +
                "  \"ClientsAddresses\": [\n" +
                "       {\"value\": {\"PostalAddress\": {\n" +
                "           \"AddressLine1\": \"Address Line 1\",\n" +
                "           \"AddressLine2\": \"Address Line 2\",\n" +
                "           \"AddressLine3\": \"Address Line 3\"\n" +
                "        }},\n" +
                "        \"id\": \"someId1\"\n" +
                "       },\n" +
                "       {\"value\": {\"PostalAddress\": {\n" +
                "           \"AddressLine1\": \"Address Line 1\",\n" +
                "           \"AddressLine2\": \"Address Line 2\",\n" +
                "           \"AddressLine3\": \"Address Line 3\"\n" +
                "        }},\n" +
                "        \"id\": \"someId2\"\n" +
                "       }\n" +
                "   ]\n" +
                "  }\n" +
                "}\n"
        ), STRING_JSON_MAP);

        final Map<String, JsonNode> classifications = caseDataService.getDefaultSecurityClassifications(caseType, DATA, Maps.newHashMap());
        final String expectedResult = "{  \n" +
            "   \"Company\":{  \n" +
            "      \"classification\":\"PUBLIC\",\n" +
            "      \"value\":{  \n" +
            "         \"PostalAddress\":{  \n" +
            "            \"classification\":\"PRIVATE\",\n" +
            "            \"value\":{  \n" +
            "               \"AddressLine1\":\"RESTRICTED\",\n" +
            "               \"AddressLine2\":\"RESTRICTED\",\n" +
            "               \"AddressLine3\":\"RESTRICTED\",\n" +
            "               \"Occupant\":{  \n" +
            "                  \"classification\":\"PUBLIC\",\n" +
            "                  \"value\":{  \n" +
            "                     \"Title\":\"PUBLIC\",\n" +
            "                     \"FirstName\":\"PUBLIC\",\n" +
            "                     \"MiddleName\":\"PRIVATE\",\n" +
            "                     \"LastName\":\"PRIVATE\",\n" +
            "                     \"DateOfBirth\":\"PRIVATE\",\n" +
            "                     \"NationalInsuranceNumber\":\"RESTRICTED\",\n" +
            "                     \"MaritalStatus\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      }\n" +
            "   },\n" +
            "   \"PersonLastName\":\"\",\n" +
            "   \"ClientsAddresses\":{  \n" +
            "      \"classification\":\"PRIVATE\",\n" +
            "      \"value\":[  \n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId1\"\n" +
            "         },\n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId2\"\n" +
            "         }\n" +
            "      ]\n" +
            "   },\n" +
            "   \"PersonFirstName\":\"\"\n" +
            "}";

        JsonNode result = MAPPER.convertValue(classifications, JsonNode.class);
        System.out.println(result);
        assertEquals(expectedResult, result.toString(), false);
    }

    @Test
    @DisplayName("should not overwrite previously set classifications")
    void shouldKeepExistingClassifications() throws IOException, JSONException {
        // ARRANGE
        final Map<String, JsonNode> DATA = MAPPER.convertValue(MAPPER.readTree(
            "{\n" +
                " \"ClientsAddresses\": " +
                "       [\n" +
                "           {\"value\": " +
                "               {\"PostalAddress\": " +
                "                   {\n" +
                "                       \"AddressLine1\": \"Address Line 1\",\n" +
                "                       \"AddressLine2\": \"Address Line 2\",\n" +
                "                       \"AddressLine3\": \"Address Line 3\"\n" +
                "                   }" +
                "           },\n" +
                "            \"id\": \"someId1\"\n" +
                "           },\n" +
                "           {\"value\": " +
                "               {" +
                "                   \"PostalAddress\": " +
                "                   {\n" +
                "                       \"AddressLine1\": \"Address Line 1\",\n" +
                "                       \"AddressLine2\": \"Address Line 2\",\n" +
                "                       \"AddressLine3\": \"Address Line 3\"\n" +
                "                   }" +
                "               },\n" +
                "            \"id\": \"someId2\"\n" +
                "           }\n" +
                "       ]\n" +
                "  }\n"
        ), STRING_JSON_MAP);
        // first to set default classification
        final Map<String, JsonNode> defaultClassifications = caseDataService.getDefaultSecurityClassifications(caseType, DATA, Maps.newHashMap());
        final String expectedDefaultResult = "{  \n" +
            "   \"ClientsAddresses\":{  \n" +
            "      \"classification\":\"PRIVATE\",\n" +
            "      \"value\":[  \n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId1\"\n" +
            "         },\n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId2\"\n" +
            "         }\n" +
            "      ]\n" +
            "   }\n" +
            "}";
        JsonNode defaultResult = MAPPER.convertValue(defaultClassifications, JsonNode.class);
        assertEquals(expectedDefaultResult, defaultResult.toString(), false);

        // then to test already set classification is not overwritten
        final Map<String, JsonNode> NEW_DATA = MAPPER.convertValue(MAPPER.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"Company\": {\n" +
                "    \"PostalAddress\": {\n" +
                "      \"AddressLine1\": \"Address Line 1\",\n" +
                "      \"AddressLine2\": \"Address Line 2\",\n" +
                "      \"AddressLine3\": \"Address Line 3\",\n" +
                "      \"Occupant\": {\n" +
                "           \"Title\": \"Mr\"," +
                "           \"FirstName\": \"John\"," +
                "           \"MiddleName\": \"Arthur\"," +
                "           \"LastName\": \"Smith\"," +
                "           \"DateOfBirth\": \"12-12-1992\"," +
                "           \"NationalInsuranceNumber\": \"SK123456D\"," +
                "           \"MaritalStatus\": \"Married\"" +
                "      }\n" +
                "     }\n" +
                "    },\n" +
                "  \"ClientsAddresses\": " +
                "       [\n" +
                "           {\"value\": " +
                "               {\"PostalAddress\": " +
                "                   {\n" +
                "                       \"AddressLine1\": \"Address Line 11\",\n" +
                "                       \"AddressLine2\": \"Address Line 21\",\n" +
                "                       \"AddressLine3\": \"Address Line 31\"\n" +
                "                   }" +
                "           },\n" +
                "            \"id\": \"someId1\"\n" +
                "           },\n" +
                "           {\"value\": " +
                "               {" +
                "                   \"PostalAddress\": " +
                "                   {\n" +
                "                       \"AddressLine1\": \"Address Line 12\",\n" +
                "                       \"AddressLine2\": \"Address Line 22\",\n" +
                "                       \"AddressLine3\": \"Address Line 32\"\n" +
                "                   }" +
                "               },\n" +
                "            \"id\": \"someId2\"\n" +
                "           }\n" +
                "       ]\n" +
                "}\n"
        ), STRING_JSON_MAP);
        setCaseType(PUBLIC);

        // ACT
        final Map<String, JsonNode> newClassifications = caseDataService.getDefaultSecurityClassifications(caseType, NEW_DATA, defaultClassifications);

        // ASSERT
        JsonNode newClassificationsResult = MAPPER.convertValue(newClassifications, JsonNode.class);
        final String expectedNewResult = "{  \n" +
            "   \"Company\":{  \n" +
            "      \"classification\":\"PUBLIC\",\n" +
            "      \"value\":{  \n" +
            "         \"PostalAddress\":{  \n" +
            "            \"classification\":\"PRIVATE\",\n" +
            "            \"value\":{  \n" +
            "               \"AddressLine1\":\"PUBLIC\",\n" +
            "               \"AddressLine2\":\"PUBLIC\",\n" +
            "               \"AddressLine3\":\"PUBLIC\",\n" +
            "               \"Occupant\":{  \n" +
            "                  \"classification\":\"PUBLIC\",\n" +
            "                  \"value\":{  \n" +
            "                     \"Title\":\"PUBLIC\",\n" +
            "                     \"FirstName\":\"PUBLIC\",\n" +
            "                     \"MiddleName\":\"PRIVATE\",\n" +
            "                     \"LastName\":\"PRIVATE\",\n" +
            "                     \"DateOfBirth\":\"PRIVATE\",\n" +
            "                     \"NationalInsuranceNumber\":\"RESTRICTED\",\n" +
            "                     \"MaritalStatus\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      }\n" +
            "   },\n" +
            "   \"PersonLastName\":\"\",\n" +
            "   \"ClientsAddresses\":{  \n" +
            "      \"classification\":\"PRIVATE\",\n" +
            "      \"value\":[  \n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId1\"\n" +
            "         },\n" +
            "         {  \n" +
            "            \"value\":{  \n" +
            "               \"PostalAddress\":{  \n" +
            "                  \"classification\":\"PRIVATE\",\n" +
            "                  \"value\":{  \n" +
            "                     \"AddressLine1\":\"RESTRICTED\",\n" +
            "                     \"AddressLine2\":\"RESTRICTED\",\n" +
            "                     \"AddressLine3\":\"RESTRICTED\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"id\": \"someId2\"\n" +
            "         }\n" +
            "      ]\n" +
            "   },\n" +
            "   \"PersonFirstName\":\"\"\n" +
            "}";
           assertEquals(expectedNewResult, newClassificationsResult.toString(), false);
    }

    @Test
    @DisplayName("should assign default classifications to simple collection items")
    void shouldAssignDefaultClassificationToCollectionItems() {
        final Map<String, JsonNode> caseData = caseData().withField("simple_collection")
                                                         .asCollectionOf(
                                                             TestBuildersUtil.collectionItem("1", "Item 1"),
                                                             TestBuildersUtil.collectionItem("2", "Item 2")
                                                          )
                                                         .build();

        final Map<String, JsonNode> classifications = caseDataService.getDefaultSecurityClassifications(
            caseType,
            caseData,
            new HashMap<>());

        assertThat(classifications.size(), equalTo(1));
        final JsonNode collection = classifications.get("simple_collection");
        assertSimpleCollectionClassification(collection,
                                             "PUBLIC",
                                             "PUBLIC", "PUBLIC");
    }

    @Test
    @DisplayName("should preserve existing classifications to simple collection items")
    void shouldPreserveExistingClassificationForCollectionItems() {
        final Map<String, JsonNode> caseData = caseData().withField("simple_collection")
                                                         .asCollectionOf(
                                                             TestBuildersUtil.collectionItem("1", "Item 1"),
                                                             TestBuildersUtil.collectionItem("2", "Item 2")
                                                          )
                                                         .build();
        final Map<String, JsonNode> existingClassification =
            dataClassification().withField("simple_collection")
                                .asCollectionOf("PRIVATE",
                                                TestBuildersUtil.collectionClassification(
                                                     "1",
                                                     "PUBLIC"),
                                                TestBuildersUtil.collectionClassification(
                                                     "2",
                                                     "RESTRICTED")
                                 )
                                .build();

        final Map<String, JsonNode> classifications = caseDataService.getDefaultSecurityClassifications(
            caseType,
            caseData,
            existingClassification);

        assertThat(classifications.size(), equalTo(1));
        final JsonNode collection = classifications.get("simple_collection");
        assertSimpleCollectionClassification(collection,
                                             "PRIVATE",
                                             "PUBLIC", "RESTRICTED");
    }

    private void assertSimpleCollectionClassification(JsonNode collection,
                                                      String expectedCollectionClassification,
                                                      String... expectedItemClassifications) {
        assertThat(collection.isObject(), is(true));
        assertThat(collection.size(), is(2));
        assertThat(collection.get("classification").textValue(), is(expectedCollectionClassification));
        final JsonNode classificationValues = collection.get("value");
        assertThat(classificationValues.isArray(), is(true));
        assertThat(classificationValues.size(), is(expectedItemClassifications.length));

        for (int i = 0; i < expectedItemClassifications.length; i++) {
            assertThat(classificationValues.get(i).get("classification").textValue(),
                       is(expectedItemClassifications[i]));
        }
    }
}
