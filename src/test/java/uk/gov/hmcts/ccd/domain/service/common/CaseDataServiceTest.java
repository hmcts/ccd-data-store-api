package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;

import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataClassificationBuilder.dataClassification;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
class CaseDataServiceTest {

    private static final CaseDataService caseDataService = new CaseDataService();
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    public void setUp() {
        setCaseTypeDefinition(RESTRICTED);
    }

    private void setCaseTypeDefinition(SecurityClassification securityClassification) {
        final FieldTypeDefinition textFieldTypeDefinition = aFieldType().withType("Text").build();

        CaseFieldDefinition postalAddress =
            newCaseField()
                .withId("PostalAddress")
                .withSC(SecurityClassification.PRIVATE.name())
                .withFieldType(aFieldType()
                    .withType("Complex")
                    .withComplexField(newCaseField()
                        .withId("AddressLine1")
                        .withFieldType(textFieldTypeDefinition)
                        .withSC(securityClassification.name())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("AddressLine2")
                        .withFieldType(textFieldTypeDefinition)
                        .withSC(securityClassification.name())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("AddressLine3")
                        .withFieldType(textFieldTypeDefinition)
                        .withSC(securityClassification.name())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("Country")
                        .withFieldType(textFieldTypeDefinition)
                        .withSC(PRIVATE.name())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("PostCode")
                        .withFieldType(textFieldTypeDefinition)
                        .withSC(RESTRICTED.name())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("Occupant")
                        .withFieldType(aFieldType()
                            .withType("Complex")
                            .withComplexField(newCaseField()
                                .withId("Title")
                                .withFieldType(textFieldTypeDefinition)
                                .withSC(PUBLIC.name()).build())
                            .withComplexField(newCaseField()
                                .withId("FirstName")
                                .withFieldType(textFieldTypeDefinition)
                                .withSC(PUBLIC.name()).build())
                            .withComplexField(newCaseField()
                                .withId("MiddleName")
                                .withFieldType(textFieldTypeDefinition)
                                .withSC(PRIVATE.name()).build())
                            .withComplexField(newCaseField()
                                .withId("LastName")
                                .withFieldType(textFieldTypeDefinition)
                                .withSC(PRIVATE.name()).build())
                            .withComplexField(newCaseField()
                                .withId("DateOfBirth")
                                .withFieldType(aFieldType().withType("Date").build())
                                .withSC(PRIVATE.name()).build())
                            .withComplexField(newCaseField()
                                .withId("NationalInsuranceNumber")
                                .withFieldType(textFieldTypeDefinition)
                                .withSC(RESTRICTED.name()).build())
                            .withComplexField(newCaseField()
                                .withId("MaritalStatus")
                                .withFieldType(aFieldType().withType("FixedList").build())
                                .withSC(RESTRICTED.name()).build())
                            .build())
                        .withSC(PUBLIC.name())
                        .build())
                    .build())
                .build();

        caseTypeDefinition = TestBuildersUtil.CaseTypeBuilder.newCaseType()
            .withField(newCaseField()
                .withId("ClientsAddresses")
                .withSC(PRIVATE.name())
                .withFieldType(aFieldType()
                    .withType(COLLECTION)
                    .withCollectionFieldType(aFieldType()
                        .withId("Address")
                        .withType("Complex")
                        .withComplexField(postalAddress)
                        .build()
                    )
                    .build())
                .build()
            )
            .withField(newCaseField()
                           .withId("Company")
                           .withSC(PUBLIC.name())
                           .withFieldType(
                               aFieldType()
                                   .withType("Complex")
                                   .withComplexField(newCaseField()
                                                         .withId("Name")
                                                         .withFieldType(textFieldTypeDefinition)
                                                         .withSC(PRIVATE.name())
                                                         .build())
                                   .withComplexField(postalAddress)
                                   .build()
                           )
                           .build()
            )
            .withField(newCaseField()
                           .withId("OtherInfo")
                           .withFieldType(textFieldTypeDefinition)
                           .withSC(PRIVATE.name())
                           .build())
            .withField(newCaseField().withId("simple_collection")
                                   .withSC("PUBLIC")
                                   .withFieldType(aFieldType().withType(COLLECTION)
                                                              .withCollectionFieldType(textFieldTypeDefinition)
                                                              .build()
                                   )
                                   .build()
            )
            .build();
    }

    @Test
    @DisplayName("should get the default security classifications")
    void shouldGetDefaultClassifications() throws IOException, JSONException {
        final Map<String, JsonNode> DATA = JacksonUtils.convertValue(MAPPER.readTree(
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
        ));

        final Map<String, JsonNode> classifications =
            caseDataService.getDefaultSecurityClassifications(caseTypeDefinition, DATA, Maps.newHashMap());
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

        JsonNode result = JacksonUtils.convertValueJsonNode(classifications);
        System.out.println(result);
        assertEquals(expectedResult, result.toString(), false);
    }

    @Test
    @DisplayName("should not overwrite previously set classifications")
    void shouldKeepExistingClassifications() throws IOException, JSONException {

        JsonNode treeValue = MAPPER.readTree(
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
        );

        final Map<String, JsonNode> DATA = JacksonUtils.convertValue(treeValue);
        // first to set default classification
        final Map<String, JsonNode> defaultClassifications =
            caseDataService.getDefaultSecurityClassifications(caseTypeDefinition, DATA, Maps.newHashMap());
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
        JsonNode defaultResult = JacksonUtils.convertValueJsonNode(defaultClassifications);
        assertEquals(expectedDefaultResult, defaultResult.toString(), false);

        // then to test already set classification is not overwritten
        final Map<String, JsonNode> NEW_DATA = JacksonUtils.convertValue(MAPPER.readTree(
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
        ));
        setCaseTypeDefinition(PUBLIC);

        // ACT
        final Map<String, JsonNode> newClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition, NEW_DATA, defaultClassifications);

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
        final Map<String, JsonNode> caseData = newCaseData().withField("simple_collection")
            .asCollectionOf(
                TestBuildersUtil.collectionItem("1", "Item 1"),
                TestBuildersUtil.collectionItem("2", "Item 2")
            )
            .build();

        final Map<String, JsonNode> classifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
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
        final Map<String, JsonNode> caseData = newCaseData().withField("simple_collection")
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
            caseTypeDefinition,
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
