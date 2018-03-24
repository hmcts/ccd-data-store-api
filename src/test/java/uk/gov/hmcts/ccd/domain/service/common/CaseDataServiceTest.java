package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.*;
import static uk.gov.hmcts.ccd.domain.service.common.CaseDataServiceTest.Field.createField;

public class CaseDataServiceTest {
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CaseDataService caseDataService = new CaseDataService();
    private CaseType caseType;

    @Before
    public void setUp() throws Exception {
        caseType = new CaseType();

        List<CaseField> caseFields = newArrayList();

        CaseField postalAddressCaseField = new CaseField();
        postalAddressCaseField.setId("PostalAddress");
        postalAddressCaseField.setSecurityLabel(SecurityClassification.PRIVATE.name());
        FieldType postalAddressCaseFieldType = new FieldType();
        postalAddressCaseFieldType.setType("Complex");
        postalAddressCaseField.setFieldType(postalAddressCaseFieldType);
        List<CaseField> postalAddressComplexFields = Lists.newArrayList();

        postalAddressComplexFields.add(setupSimpleField(
            createField("AddressLine1", "Text", RESTRICTED)));
        postalAddressComplexFields.add(setupSimpleField(
            createField("AddressLine2", "Text", RESTRICTED)));
        postalAddressComplexFields.add(setupSimpleField(
            createField("AddressLine3", "Text", RESTRICTED)));
        postalAddressComplexFields.add(setupSimpleField(
            createField("Country", "Text", PRIVATE)));
        postalAddressComplexFields.add(setupSimpleField(
            createField("PostCode", "Postcode", RESTRICTED)));
        postalAddressComplexFields.add(setupComplexField(
            "Occupant", PUBLIC,
            createField("Title", "Text", PUBLIC),
            createField("FirstName", "Text", PUBLIC),
            createField("MiddleName", "Text", PRIVATE),
            createField("LastName", "Text", PRIVATE),
            createField("DateOfBirth", "Date", PRIVATE),
            createField("NationalInsuranceNumber", "Text", RESTRICTED),
            createField("MaritalStatus", "FixedList", RESTRICTED)));
        postalAddressCaseFieldType.setComplexFields(postalAddressComplexFields);

        CaseField companyField = new CaseField();
        companyField.setId("Company");
        companyField.setSecurityLabel(SecurityClassification.PUBLIC.name());
        FieldType companyFieldType = new FieldType();
        companyFieldType.setType("Complex");
        List<CaseField> companyComplexFields = Lists.newArrayList();

        companyComplexFields.add(
            setupSimpleField(createField("Name", "Text", PRIVATE)));
        companyComplexFields.add(
            postalAddressCaseField);
        companyFieldType.setComplexFields(companyComplexFields);
        companyField.setFieldType(companyFieldType);

        caseFields.add(companyField);

        caseFields.add(setupSimpleField(
            createField("OtherInfo", "Text", PRIVATE)
        ));

        CaseField clientsAddressesField = setupCollectionField(postalAddressCaseField);

        caseFields.add(clientsAddressesField);

        caseType.setCaseFields(caseFields);
    }

    private CaseField setupCollectionField(CaseField postalAddressCaseField) {
        CaseField clientsAddressesField = new CaseField();
        clientsAddressesField.setSecurityLabel(SecurityClassification.PRIVATE.name());
        clientsAddressesField.setId("ClientsAddresses");
        FieldType clientsAddressesFieldType = new FieldType();
        clientsAddressesFieldType.setType("Collection");
        FieldType clientsAddressesCollectionFieldType = new FieldType();
        clientsAddressesCollectionFieldType.setId("Address");
        clientsAddressesCollectionFieldType.setType("Complex");
        clientsAddressesFieldType.setCollectionFieldType(clientsAddressesCollectionFieldType);
        clientsAddressesField.setFieldType(clientsAddressesFieldType);
        List<CaseField> clientsAddressesComplexFields = Lists.newArrayList();
        clientsAddressesComplexFields.add(postalAddressCaseField);
        clientsAddressesCollectionFieldType.setComplexFields(clientsAddressesComplexFields);
        return clientsAddressesField;
    }

    private CaseField setupSimpleField(Field field) {
        CaseField parentField = new CaseField();
        parentField.setId(field.id);
        parentField.setSecurityLabel(field.securityClassification.name());
        FieldType parentFieldType = new FieldType();
        parentFieldType.setId(field.id);
        parentFieldType.setType(field.fieldType);
        parentField.setFieldType(parentFieldType);
        return parentField;
    }

    private CaseField setupComplexField(String parentFieldId, SecurityClassification securityClassification, Field... fields) {
        List<CaseField> complexFieldsList = Lists.newArrayList();
        Lists.newArrayList(fields)
            .forEach(field -> {
                CaseField caseField = new CaseField();
                caseField.setId(field.id);
                caseField.setSecurityLabel(field.securityClassification.name());
                FieldType fieldType = new FieldType();
                fieldType.setType(field.fieldType);
                caseField.setFieldType(fieldType);
                complexFieldsList.add(caseField);
            });
        CaseField parentField = new CaseField();
        parentField.setId(parentFieldId);
        parentField.setSecurityLabel(securityClassification.name());
        FieldType parentFieldType = new FieldType();
        parentFieldType.setId(parentFieldId);
        parentFieldType.setComplexFields(complexFieldsList);
        parentFieldType.setType("Complex");
        parentField.setFieldType(parentFieldType);
        return parentField;
    }

    static class Field {
        final String id;
        final String fieldType;
        final SecurityClassification securityClassification;

        Field(String id, String fieldType, SecurityClassification securityClassification) {
            this.id = id;
            this.fieldType = fieldType;
            this.securityClassification = securityClassification;
        }

        static Field createField(String id, String fieldType, SecurityClassification securityClassification) {
            return new Field(id, fieldType, securityClassification);
        }
    }

    @Test
    public void testGetDefaultSecurityClassifications() throws IOException, JSONException {
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

        final Map<String, JsonNode> classifications = caseDataService.getDefaultSecurityClassifications(caseType, DATA);
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

}
