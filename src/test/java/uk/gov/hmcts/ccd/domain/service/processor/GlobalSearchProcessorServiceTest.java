package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.definition.SearchParty;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.SEARCH_CRITERIA;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchCriteriaFields.OTHER_CASE_REFERENCES;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchCriteriaFields.SEARCH_PARTIES;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.DATE_OF_BIRTH;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.DATE_OF_DEATH;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.EMAIL_ADDRESS;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.NAME;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchPartyFields.POSTCODE;

class GlobalSearchProcessorServiceTest {

    private GlobalSearchProcessorService globalSearchProcessorService;
    private CaseTypeDefinition caseTypeDefinition;
    private Map<String, JsonNode> caseData;
    private List<String> searchPartyPropertyNames;
    private SearchParty searchParty;

    private static final String ID = "id";
    private static final String VALUE = "value";

    private static final String SEARCH_PARTY_FIRST_NAME_1 = "John";
    private static final String SEARCH_PARTY_MIDDLE_NAME_1 = "Johnny";
    private static final String SEARCH_PARTY_LAST_NAME_1 = "Johnson";
    private static final String SEARCH_PARTY_DOB_1 = "1979-05-16";
    private static final String SEARCH_PARTY_DOD_1 = "2000-05-16";
    private static final String SEARCH_PARTY_ADDRESS_1 = "My Address";
    private static final String SEARCH_PARTY_POSTCODE_1 = "AB1 2CD";
    private static final String SEARCH_PARTY_EMAIL_1 = "a@b.com";

    private static final String SEARCH_PARTY_FIRST_NAME_2 = "John";
    private static final String SEARCH_PARTY_DOB_2 = "1980-05-16";
    private static final String SEARCH_PARTY_DOD_2 = "2001-05-16";
    private static final String SEARCH_PARTY_ADDRESS_2 = "My 2nd Address";
    private static final String SEARCH_PARTY_POSTCODE_2 = "EF3 4GH";
    private static final String SEARCH_PARTY_EMAIL_2 = "c@d.com";

    private final ObjectMapperService objectMapperService = new DefaultObjectMapperService(new ObjectMapper());

    @BeforeEach
    void setup() throws JsonProcessingException {

        globalSearchProcessorService = new GlobalSearchProcessorService(objectMapperService);
        caseTypeDefinition = new CaseTypeDefinition();

        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(SEARCH_CRITERIA);

        List<CaseFieldDefinition> caseFieldDefinitions = List.of(caseFieldDefinition);

        caseTypeDefinition.setCaseFieldDefinitions(caseFieldDefinitions);

        caseData = new HashMap<>();
        caseData.put("PersonFirstName", JacksonUtils.MAPPER.readTree("\"FirstNameValue\""));

        searchPartyPropertyNames = List.of(NAME, ADDRESS_LINE_1, EMAIL_ADDRESS, POSTCODE, DATE_OF_BIRTH, DATE_OF_DEATH);
        searchParty = new SearchParty();
    }

    @Test
    void checkGlobalSearchFunctionalityDisabledWithoutAppropriateCaseField() {
        caseTypeDefinition.setCaseFieldDefinitions(new ArrayList<>());
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);
        assertNull(globalSearchData.get(SEARCH_CRITERIA));
    }

    @ParameterizedTest(name = "Should return empty SearchCriteria for empty or null case data: {0}")
    @NullAndEmptySource
    void checkNullAndEmptyCaseDataResultsInEmptySearchCriteriaCreation(Map<String, JsonNode> testData) {

        // ARRANGE
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setOtherCaseReference("TextField1");

        SearchParty searchParty = new SearchParty();
        searchParty.setSearchPartyName("TextField1");

        caseTypeDefinition.setSearchCriterias(List.of(searchCriteria));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        // ACT / ASSERT
        assertSearchCriteriaCreatedButEmpty(testData);

    }

    @Test
    void searchCriteriaStillCreatedEvenIfCollectionIsMissing()  {

        final String collectionName = "myCollection";
        final String firstName = "FirstName";
        final String middleName = "MiddleName";
        final String lastName = "LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        // NB: not adding `myCollection` field to case data as test requires it to be missing

        // ACT / ASSERT
        assertSearchCriteriaCreatedButEmpty(caseData);
    }

    @ParameterizedTest(name = "SearchCriteria is still created even if collection is null, empty or bad: {0}")
    @ValueSource(strings = {
        "null",                      // run 1: null
        "[]",                        // run 2: empty
        "{ \"FirstName\": \"1\" }",  // run 3: random object
        "1234",                      // run 4: not an object
        "[\n"                        // run 5: array of bad items
            + "  null,\n"            //   - 5.1: item is null
            + "  1234,\n"            //   - 5.2: item is not an object
            + "  {\n"                //   - 5.3: item has no value field
            + "    \"id\": null\n"
            + "  },\n"
            + "  {\n"                //   - 5.4: item has value, but it is null
            + "    \"id\": null,\n"
            + "    \"value\": null\n"
            + "  },\n"
            + "  {\n"               //    - 5.5: item has value, but it is another collection/array
            + "    \"id\": null,\n"
            + "    \"value\": []\n"
            + "  },\n"
            + "  {\n"              //     - 5.6: item has value, but it is not an object
            + "    \"id\": null,\n"
            + "    \"value\": 1234\n"
            + "  },\n"
            + "  {\n"              //     - 5.7: item is legal but properties are null, missing or empty
            + "    \"id\": null,\n"
            + "    \"value\": {\n"
            + "      \"FirstName\": null,\n"   // null name part
            //                                    missing name part (MiddleName)
            + "      \"LastName\": \"\",\n"    // empty name part
            + "      \"DoB\": null,\n"         // null other
            //                                    missing other (DoD)
            + "      \"AddressLine1\": \"\"\n" // empty other
            + "    }\n"
            + "  }\n"
            + "]"
    })
    void searchCriteriaStillCreatedEvenIfCollectionIsNullEmptyOrBad(String collectionAsString)
        throws JsonProcessingException {

        final String collectionName = "myCollection";
        final String firstName = "FirstName";
        final String middleName = "MiddleName";
        final String lastName = "LastName";
        final String dob = "DoB";
        final String dod = "DoD";
        final String postCode = "PostCode";
        final String email = "EmailAddress";
        final String address = "AddressLine1";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyAddressLine1(address);
        searchParty.setSearchPartyPostCode(postCode);
        searchParty.setSearchPartyEmailAddress(email);
        searchParty.setSearchPartyDob(dob);
        searchParty.setSearchPartyDod(dod);
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("myCollection", JacksonUtils.MAPPER.readTree(collectionAsString));

        // ACT / ASSERT
        assertSearchCriteriaCreatedButEmpty(caseData);
    }

    @Test
    void checkOtherReferenceFieldPopulatedInSearchCriteria() throws JsonProcessingException {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setOtherCaseReference("TextField1");

        caseTypeDefinition.setSearchCriterias(List.of(searchCriteria));

        final String lastName = "LastNameValue";
        caseData.put("TextField1", JacksonUtils.MAPPER.readTree("\""  + lastName  + "\""));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);


        JsonNode searchCriteriaNode = globalSearchData.get(SEARCH_CRITERIA);
        assertEquals(lastName, searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(VALUE).asText());
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.findValue(ID).asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(ID).asText()));
        assertFalse(searchCriteriaNode.has(SEARCH_PARTIES));
    }

    @Test
    void checkOtherReferenceFieldPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setOtherCaseReference("PersonAddress.AddressLine1");

        caseTypeDefinition.setSearchCriterias(List.of(searchCriteria));

        final String addressValue = "This is my address";

        caseData.put("PersonAddress", JacksonUtils.MAPPER.readTree("{\"AddressLine1\": \"" + addressValue  + "\"}"));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);
        JsonNode searchCriteriaNode = globalSearchData.get(SEARCH_CRITERIA);

        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.findValue(ID).asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(ID).asText()));
        assertEquals(addressValue, searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(VALUE).asText());
        assertFalse(globalSearchData.get(SEARCH_CRITERIA).has(SEARCH_PARTIES));
    }

    @Test
    void checkOtherReferenceFieldPopulatedComplexTypeInSearchCriteriaWithNullValues() throws JsonProcessingException {

        SearchCriteria searchCriteria1 = new SearchCriteria();
        searchCriteria1.setOtherCaseReference("PersonAddress.AddressLine1");
        SearchCriteria searchCriteria2 = new SearchCriteria();
        searchCriteria2.setOtherCaseReference("Person.Name");

        caseTypeDefinition.setSearchCriterias(List.of(searchCriteria1, searchCriteria2));

        final String addressValue = "My address line 1";

        caseData.put("PersonAddress", JacksonUtils.MAPPER.readTree("{\"AddressLine1\": \"" + addressValue  + "\"}"));
        caseData.put("Person", JacksonUtils.MAPPER.readTree("{\"Name\": null}"));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);
        JsonNode searchCriteriaNode = globalSearchData.get(SEARCH_CRITERIA);

        assertEquals(1, searchCriteriaNode.size());
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.findValue(ID).asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(ID).asText()));
        assertEquals(addressValue, searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(VALUE).asText());
        assertFalse(globalSearchData.get(SEARCH_CRITERIA).has(SEARCH_PARTIES));
    }

    @Test
    void checkOtherReferenceFieldPopulatedSimpleFieldInSearchCriteriaWithNullValues() throws JsonProcessingException {

        SearchCriteria searchCriteria1 = new SearchCriteria();
        searchCriteria1.setOtherCaseReference("caseReference");
        SearchCriteria searchCriteria2 = new SearchCriteria();
        searchCriteria2.setOtherCaseReference("caseName");

        caseTypeDefinition.setSearchCriterias(List.of(searchCriteria1, searchCriteria2));

        final String caseReference = "123456789";

        caseData.put("caseReference", JacksonUtils.convertValueJsonNode(caseReference));
        caseData.put("caseName", JacksonUtils.MAPPER.nullNode());

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);
        JsonNode searchCriteriaNode = globalSearchData.get(SEARCH_CRITERIA);

        assertEquals(1, searchCriteriaNode.size());
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.findValue(ID).asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(ID).asText()));
        assertEquals(caseReference, searchCriteriaNode.get(OTHER_CASE_REFERENCES).findValue(VALUE).asText());
        assertFalse(globalSearchData.get(SEARCH_CRITERIA).has(SEARCH_PARTIES));
    }

    @Test
    void checkSearchPartyNamePopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyName("SearchPartyFirstName");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyFirstName", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_FIRST_NAME_1  + "\""));

        assertSearchCriteriaField(NAME, SEARCH_PARTY_FIRST_NAME_1);
    }

    @Test
    void checkSearchPartyNamePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyName("SearchParty.FirstName");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchParty",
            JacksonUtils.MAPPER.readTree("{\"FirstName\": \"" + SEARCH_PARTY_FIRST_NAME_1  + "\"}"));

        assertSearchCriteriaField(NAME, SEARCH_PARTY_FIRST_NAME_1);
    }

    @Test
    void checkCommaSeparatedValuesSearchPartyNamePopulatedInSearchCriteria() throws JsonProcessingException {
        final String searchPartyNameFirstName = "John";
        final String searchPartyNameMiddleName = "Johnny";
        final String searchPartyNameLastName = "Johnson";

        final String firstName = "FirstName";
        final String middleName = "MiddleName";
        final String lastName = "LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put(firstName, JacksonUtils.MAPPER.readTree("\""  + searchPartyNameFirstName  + "\""));
        caseData.put(middleName, JacksonUtils.MAPPER.readTree("\""  + searchPartyNameMiddleName  + "\""));
        caseData.put(lastName, JacksonUtils.MAPPER.readTree("\""  + searchPartyNameLastName  + "\""));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME, String.format("%s %s %s",
            searchPartyNameFirstName,
            searchPartyNameMiddleName,
            searchPartyNameLastName));

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkCommaSeparatedValuesSearchPartyNameWithNullAndMissingValuesPopulatedInSearchCriteria()
        throws JsonProcessingException {

        final String searchPartyNameFirstName = "John";
        final String searchPartyNameLastName = "Johnson";

        final String firstName = "FirstName";
        final String middleName = "MiddleName"; // test will set this field to null in case data
        final String missingName = "MissingName"; // test will miss this field out of the case data
        final String lastName = "LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s,%s", firstName, middleName, missingName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put(firstName, JacksonUtils.MAPPER.readTree("\""  + searchPartyNameFirstName  + "\""));
        caseData.put(middleName, JacksonUtils.MAPPER.readTree("null"));
        caseData.put(lastName, JacksonUtils.MAPPER.readTree("\""  + searchPartyNameLastName  + "\""));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME, String.format("%s %s",
            searchPartyNameFirstName,
            searchPartyNameLastName)); // i.e. only the two values returned with no extra spaces for missing values

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkCommaSeparatedComplexValuesSearchPartyNamePopulatedInSearchCriteria() throws JsonProcessingException {

        final String firstName = "MyPerson.Person.FirstName";
        final String middleName = "MyPerson.Person.MiddleName";
        final String lastName = "MyPerson.Person.LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("MyPerson", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"Person\":{\n"
            + "         \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "         \"MiddleName\":\"" + SEARCH_PARTY_MIDDLE_NAME_1 + "\",\n"
            + "         \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));


        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", SEARCH_PARTY_FIRST_NAME_1, SEARCH_PARTY_MIDDLE_NAME_1, SEARCH_PARTY_LAST_NAME_1));

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkSearchPartyValuesPopulatedInSearchCriteriaFromCollection()
        throws JsonProcessingException {

        final String collectionName = "myCollection";
        final String firstName = "FirstName";
        final String middleName = "MiddleName";
        final String lastName = "LastName";
        final String dob = "DoB";
        final String dod = "DoD";
        final String postCode = "PostCode";
        final String email = "EmailAddress";
        final String address = "AddressLine1";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyAddressLine1(address);
        searchParty.setSearchPartyPostCode(postCode);
        searchParty.setSearchPartyEmailAddress(email);
        searchParty.setSearchPartyDob(dob);
        searchParty.setSearchPartyDod(dod);
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("myCollection", JacksonUtils.MAPPER.readTree("[{\n"
            + "         \"id\": null,\n"
            + "         \"value\": {\n"
            + "             \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "             \"MiddleName\":\"" + SEARCH_PARTY_MIDDLE_NAME_1 + "\",\n"
            + "             \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\",\n"
            + "             \"DoB\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "             \"DoD\":\"" + SEARCH_PARTY_DOD_1 + "\",\n"
            + "             \"AddressLine1\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "             \"PostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\",\n"
            + "             \"EmailAddress\":\"" + SEARCH_PARTY_EMAIL_1 + "\"\n"
            + "         }\n"
            + "      }]\n"));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", SEARCH_PARTY_FIRST_NAME_1, SEARCH_PARTY_MIDDLE_NAME_1, SEARCH_PARTY_LAST_NAME_1));
        expectedValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
        expectedValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkComplexSearchPartyValuesPopulatedInSearchCriteriaFromCollection()
        throws JsonProcessingException {

        final String collectionName = "myCollection";
        final String firstName = "Person.FirstName";
        final String middleName = "Person.MiddleName";
        final String lastName = "Person.LastName";
        final String dob = "Person.DoB";
        final String dod = "Person.DoD";
        final String postCode = "Person.Address.PostCode";
        final String email = "Person.EmailAddress";
        final String address = "Person.Address.AddressLine1";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyAddressLine1(address);
        searchParty.setSearchPartyPostCode(postCode);
        searchParty.setSearchPartyEmailAddress(email);
        searchParty.setSearchPartyDob(dob);
        searchParty.setSearchPartyDod(dod);
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("myCollection", JacksonUtils.MAPPER.readTree("[{\n"
            + "         \"id\": null,\n"
            + "         \"value\": {\n"
            + "             \"Person\": {\n"
            + "                 \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "                 \"MiddleName\":\"" + SEARCH_PARTY_MIDDLE_NAME_1 + "\",\n"
            + "                 \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\",\n"
            + "                 \"DoB\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "                 \"DoD\":\"" + SEARCH_PARTY_DOD_1 + "\",\n"
            + "                 \"Address\": {\n"
            + "                     \"AddressLine1\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "                     \"PostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\"\n"
            + "                 },\n"
            + "                 \"EmailAddress\":\"" + SEARCH_PARTY_EMAIL_1 + "\"\n"
            + "             }\n"
            + "          }\n"
            + "      }]\n"));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", SEARCH_PARTY_FIRST_NAME_1, SEARCH_PARTY_MIDDLE_NAME_1, SEARCH_PARTY_LAST_NAME_1));
        expectedValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
        expectedValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkComplexSearchPartyValuesPopulatedInSearchCriteriaFromComplexCollection()
        throws JsonProcessingException {

        final String collectionName = "myComplex.myCollection";
        final String firstName = "Person.FirstName";
        final String middleName = "Person.MiddleName";
        final String lastName = "Person.LastName";
        final String dob = "Person.DoB";
        final String dod = "Person.DoD";
        final String postCode = "Person.Address.PostCode";
        final String email = "Person.EmailAddress";
        final String address = "Person.Address.AddressLine1";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyAddressLine1(address);
        searchParty.setSearchPartyPostCode(postCode);
        searchParty.setSearchPartyEmailAddress(email);
        searchParty.setSearchPartyDob(dob);
        searchParty.setSearchPartyDod(dod);
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("myComplex", JacksonUtils.MAPPER.readTree("{\n"
            + "         \"myCollection\": [{\n"
            + "            \"id\": null,\n"
            + "             \"value\": {\n"
            + "                 \"Person\": {\n"
            + "                     \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "                     \"MiddleName\":\"" + SEARCH_PARTY_MIDDLE_NAME_1 + "\",\n"
            + "                     \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\",\n"
            + "                     \"DoB\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "                     \"DoD\":\"" + SEARCH_PARTY_DOD_1 + "\",\n"
            + "                     \"Address\": {\n"
            + "                         \"AddressLine1\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "                         \"PostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\"\n"
            + "                     },\n"
            + "                     \"EmailAddress\":\"" + SEARCH_PARTY_EMAIL_1 + "\"\n"
            + "                 }\n"
            + "             }\n"
            + "         }]\n"
            + "     }\n"));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", SEARCH_PARTY_FIRST_NAME_1, SEARCH_PARTY_MIDDLE_NAME_1, SEARCH_PARTY_LAST_NAME_1));
        expectedValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
        expectedValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkSearchPartyValuesPopulatedInSearchCriteriaFromComplexCollection()
        throws JsonProcessingException {

        final String collectionName = "myComplex.myCollection";
        final String firstName = "FirstName";
        final String middleName = "MiddleName";
        final String lastName = "LastName";
        final String dob = "DoB";
        final String dod = "DoD";
        final String postCode = "Address.PostCode";
        final String email = "EmailAddress";
        final String address = "Address.AddressLine1";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        searchParty.setSearchPartyAddressLine1(address);
        searchParty.setSearchPartyPostCode(postCode);
        searchParty.setSearchPartyEmailAddress(email);
        searchParty.setSearchPartyDob(dob);
        searchParty.setSearchPartyDod(dod);
        searchParty.setSearchPartyCollectionFieldName(collectionName);
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("myComplex", JacksonUtils.MAPPER.readTree("{\n"
            + "         \"myCollection\": [{\n"
            + "            \"id\": null,\n"
            + "            \"value\": {\n"
            + "                \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "                \"MiddleName\":\"" + SEARCH_PARTY_MIDDLE_NAME_1 + "\",\n"
            + "                \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\",\n"
            + "                \"DoB\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "                \"DoD\":\"" + SEARCH_PARTY_DOD_1 + "\",\n"
            + "                 \"Address\": {\n"
            + "                     \"AddressLine1\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "                     \"PostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\"\n"
            + "                 },\n"
            + "                \"EmailAddress\":\"" + SEARCH_PARTY_EMAIL_1 + "\"\n"
            + "            }\n"
            + "         }]\n"
            + "     }\n"));

        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", SEARCH_PARTY_FIRST_NAME_1, SEARCH_PARTY_MIDDLE_NAME_1, SEARCH_PARTY_LAST_NAME_1));
        expectedValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
        expectedValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkCommaSeparatedComplexValuesSearchPartyNameWithNullAndMissingValuesPopulatedInSearchCriteria()
        throws JsonProcessingException {

        final String firstName = "MyPerson.Person.FirstName";
        final String middleName = "MyPerson.Person.MiddleName"; // test will set this field to null in case data
        final String missingName = "MyPerson.Person.MissingName"; // test will miss this field out of the case data
        final String lastName = "MyPerson.Person.LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s,%s", firstName, middleName, missingName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("MyPerson", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"Person\":{\n"
            + "         \"FirstName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "         \"MiddleName\": null,\n"
            + "         \"LastName\":\"" + SEARCH_PARTY_LAST_NAME_1 + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));


        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s",
                SEARCH_PARTY_FIRST_NAME_1,
                SEARCH_PARTY_LAST_NAME_1)); // i.e. only the two values returned with no extra spaces for missing values

        assertSearchCriteriaFields(expectedValues);
    }


    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteria() throws JsonProcessingException {
        searchParty.setSearchPartyAddressLine1("SearchPartyAddress");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_ADDRESS_1  + "\""));

        assertSearchCriteriaField(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
    }

    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyAddressLine1("SearchParty.Address");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\"Address\":\"" + SEARCH_PARTY_ADDRESS_1  + "\"}"));

        assertSearchCriteriaField(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
    }

    @Test
    void checkSearchPartyEMailAddressPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyEmailAddress("SearchPartyEmailAddress");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyEmailAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_EMAIL_1  + "\""));

        assertSearchCriteriaField(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
    }

    @Test
    void checkSearchPartyEMailAddressPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyEmailAddress("SearchParty.EmailAddress");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchParty",
            JacksonUtils.MAPPER.readTree("{\"EmailAddress\": \"" + SEARCH_PARTY_EMAIL_1  + "\"}"));

        assertSearchCriteriaField(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
    }

    @Test
    void checkSearchPartyPostCodePopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyPostCode("SearchPartyPostCode");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyPostCode", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_POSTCODE_1  + "\""));

        assertSearchCriteriaField(POSTCODE, SEARCH_PARTY_POSTCODE_1);
    }

    @Test
    void checkSearchPartyPostCodePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyPostCode("MySearchParty.SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("MySearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"SearchParty\":{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + SEARCH_PARTY_POSTCODE_1 + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));

        assertSearchCriteriaField(POSTCODE, SEARCH_PARTY_POSTCODE_1);
    }

    @Test
    void checkSearchPartyDateOfDeathPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchPartyDod");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyDod", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOD_1  + "\""));

        assertSearchCriteriaField(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
    }

    @Test
    void checkSearchPartyDateOfDeathEmptyInSearchCriteriaWhenBadDateValue() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchPartyDod");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dodField = "bad-dob";
        caseData.put("SearchPartyDod", JacksonUtils.MAPPER.readTree("\""  + dodField  + "\""));

        assertSearchCriteriaCreatedButEmpty(caseData);
    }

    @Test
    void checkSearchPartyDateOfDeathPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + SEARCH_PARTY_DOD_1 + "\"\n"
            + "      }\n"
            + "}"));

        assertSearchCriteriaField(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchPartyDoB");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchPartyDoB", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOB_1  + "\""));

        assertSearchCriteriaField(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + SEARCH_PARTY_DOB_1 + "\"\n"
            + "      }\n"
            + "}"));

        assertSearchCriteriaField(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
    }

    @Test
    void checkSearchPartyDateOfBirthEmptyInSearchCriteriaWhenBadDateValue() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchPartyDob");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dobField = "bad-dob";
        caseData.put("SearchPartyDob", JacksonUtils.MAPPER.readTree("\""  + dobField  + "\""));

        assertSearchCriteriaCreatedButEmpty(caseData);
    }

    @Test
    void checkSearchCriteriaExistsButSearchPartyNodeIsNullWhenTwoBadDateValues()
        throws JsonProcessingException {

        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "bad-dob";
        final String dod = "bad-dod";

        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        assertSearchCriteriaCreatedButEmpty(caseData);
    }

    @Test
    void checkSearchCriteriaContainingASearchPartyContainingAllFields() throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, SEARCH_PARTY_FIRST_NAME_1);
        expectedFieldValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedFieldValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedFieldValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);
        expectedFieldValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedFieldValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_FIRST_NAME_1  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_ADDRESS_1  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_POSTCODE_1  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_EMAIL_1  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOB_1  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOD_1  + "\""));

        assertSearchCriteriaFields(expectedFieldValues);
    }

    @Test
    void checkSearchCriteriaContainingASearchPartyContainingAllFieldsFromCollection() throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");
        searchParty.setSearchPartyCollectionFieldName("myCollection");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, SEARCH_PARTY_FIRST_NAME_1);
        expectedFieldValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedFieldValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedFieldValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);
        expectedFieldValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedFieldValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);

        caseData.put("myCollection", JacksonUtils.MAPPER.readTree("[{\n"
            + "         \"id\": null,\n"
            + "         \"value\": {\n"
            + "             \"spName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "             \"spAddress\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "             \"spPostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\",\n"
            + "             \"spEmail\":\"" + SEARCH_PARTY_EMAIL_1 + "\",\n"
            + "             \"spDob\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "             \"spDod\":\"" + SEARCH_PARTY_DOD_1 + "\"\n"
            + "         }\n"
            + "      }]\n"
            + "}"));

        assertSearchCriteriaFields(expectedFieldValues);
    }

    @Test
    void checkSearchCriteriaContainingASearchPartyContainingAllFieldsTwoDateFieldsAreWrong()
        throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, SEARCH_PARTY_FIRST_NAME_1);
        expectedFieldValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedFieldValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedFieldValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_FIRST_NAME_1  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_ADDRESS_1  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_POSTCODE_1  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_EMAIL_1  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\"bad-dob\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\"bad-dod\""));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNodes = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        // verify only fields returned (i.e. 6 minus the 2 dates: dataOfBirth and dateOfDeath)
        assertEquals(4, searchPartyNodes.findValue(VALUE).size());

        assertSearchCriteriaFields(expectedFieldValues);
    }

    @Test
    void checkSearchCriteriaContainingMultipleSearchPartiesContainingAllFields() throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        SearchParty searchParty2 = new SearchParty();
        searchParty2.setSearchPartyName("sp.Name.value");
        searchParty2.setSearchPartyDob("sp.Dob.value");
        searchParty2.setSearchPartyPostCode("sp.PostCode.value");
        searchParty2.setSearchPartyEmailAddress("sp.Email.value");
        searchParty2.setSearchPartyAddressLine1("sp.Address.value");

        caseTypeDefinition.setSearchParties(List.of(searchParty, searchParty2));

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_FIRST_NAME_1  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_ADDRESS_1  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_POSTCODE_1  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_EMAIL_1  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOB_1  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOD_1  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_FIRST_NAME_2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_DOB_2 + "\"\n"
            + "      },\n"
            + "      \"Dod\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_DOD_2 + "\"\n"
            + "      },\n"
            + "      \"Address\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_ADDRESS_2 + "\"\n"
            + "      },\n"
            + "      \"Email\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_EMAIL_2 + "\"\n"
            + "      },\n"
            + "      \"PostCode\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_POSTCODE_2 + "\"\n"
            + "      }\n"
            + "}"));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNodes = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertEquals(2, searchPartyNodes.size());
    }

    @Test
    void checkSearchCriteriaContainsMultipleSearchPartiesFromCollection()
        throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");
        searchParty.setSearchPartyCollectionFieldName("myCollection");

        SearchParty searchParty2 = new SearchParty();
        searchParty2.setSearchPartyName("sp.Name.value");
        searchParty2.setSearchPartyDob("sp.Dob.value");
        searchParty2.setSearchPartyPostCode("sp.PostCode.value");
        searchParty2.setSearchPartyEmailAddress("sp.Email.value");
        searchParty2.setSearchPartyAddressLine1("sp.Address.value");
        searchParty2.setSearchPartyAddressLine1("sp.Collection.value");

        caseTypeDefinition.setSearchParties(List.of(searchParty, searchParty2));

        caseData.put("myCollection", JacksonUtils.MAPPER.readTree("[{\n"
            + "         \"id\": null,\n"
            + "         \"value\": {\n"
            + "             \"spName\":\"" + SEARCH_PARTY_FIRST_NAME_1 + "\",\n"
            + "             \"spAddress\":\"" + SEARCH_PARTY_ADDRESS_1 + "\",\n"
            + "             \"spPostCode\":\"" + SEARCH_PARTY_POSTCODE_1 + "\",\n"
            + "             \"spEmail\":\"" + SEARCH_PARTY_EMAIL_1 + "\",\n"
            + "             \"spDob\":\"" + SEARCH_PARTY_DOB_1 + "\",\n"
            + "             \"spDod\":\"" + SEARCH_PARTY_DOD_1 + "\"\n"
            + "         }\n"
            + "      },\n"
            + "      {\n"
            + "         \"id\": null,\n"
            + "         \"value\": {\n"
            + "             \"spName\":\"" + SEARCH_PARTY_FIRST_NAME_2 + "\",\n"
            + "             \"spAddress\":\"" + SEARCH_PARTY_ADDRESS_2 + "\",\n"
            + "             \"spPostCode\":\"" + SEARCH_PARTY_POSTCODE_2 + "\",\n"
            + "             \"spEmail\":\"" + SEARCH_PARTY_EMAIL_2 + "\",\n"
            + "             \"spDob\":\"" + SEARCH_PARTY_DOB_2 + "\",\n"
            + "             \"spDod\":\"" + SEARCH_PARTY_DOD_2 + "\"\n"
            + "         }\n"
            + "    }]\n"
            + "}"));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNodes = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertEquals(2, searchPartyNodes.size());
    }

    @Test
    void checkSearchCriteriaContainingSingleMatchingSearchPartyContainingAllFields() throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        SearchParty searchParty2 = new SearchParty();
        searchParty2.setSearchPartyName("do.not.find.me");
        searchParty2.setSearchPartyDob("do.not.find.me");
        searchParty2.setSearchPartyDod("do.not.find.me");
        searchParty2.setSearchPartyPostCode("do.not.find.me");
        searchParty2.setSearchPartyEmailAddress("do.not.find.me");
        searchParty2.setSearchPartyAddressLine1("do.not.find.me");

        caseTypeDefinition.setSearchParties(List.of(searchParty, searchParty2));

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, SEARCH_PARTY_FIRST_NAME_1);
        expectedFieldValues.put(ADDRESS_LINE_1, SEARCH_PARTY_ADDRESS_1);
        expectedFieldValues.put(EMAIL_ADDRESS, SEARCH_PARTY_EMAIL_1);
        expectedFieldValues.put(POSTCODE, SEARCH_PARTY_POSTCODE_1);
        expectedFieldValues.put(DATE_OF_BIRTH, SEARCH_PARTY_DOB_1);
        expectedFieldValues.put(DATE_OF_DEATH, SEARCH_PARTY_DOD_1);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_FIRST_NAME_1  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_ADDRESS_1  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_POSTCODE_1  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_EMAIL_1  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOB_1  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + SEARCH_PARTY_DOD_1  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_FIRST_NAME_2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_DOB_2 + "\"\n"
            + "      },\n"
            + "      \"Dod\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_DOD_2 + "\"\n"
            + "      },\n"
            + "      \"Address\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_ADDRESS_2 + "\"\n"
            + "      },\n"
            + "      \"Email\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_EMAIL_2 + "\"\n"
            + "      },\n"
            + "      \"PostCode\":{\n"
            + "         \"value\":\"" + SEARCH_PARTY_POSTCODE_2 + "\"\n"
            + "      }\n"
            + "}"));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNodes = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertEquals(1, searchPartyNodes.size());

        JsonNode searchPartyNode = searchPartyNodes.get(0);

        expectedFieldValues.forEach((key, value) ->
            assertEquals(value, searchPartyNode.findValue(VALUE).findValue(key).asText()));
    }

    private void assertSearchCriteriaField(String fieldName, Object expectedValue) {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNode = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertDoesNotThrow(() -> UUID.fromString(searchPartyNode.findValue(ID).asText()));
        assertEquals(expectedValue, searchPartyNode.findValue(VALUE).findValue(fieldName).asText());

        searchPartyPropertyNames.stream()
            .filter(propertyName -> !propertyName.equals(fieldName))
            .forEach(field -> assertFalse(searchPartyNode.findValue(VALUE).has(field)));
    }

    private void assertSearchCriteriaFields(Map<String, String> expectedValues) {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNode = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertDoesNotThrow(() -> UUID.fromString(searchPartyNode.findValue(ID).asText()));

        expectedValues.forEach((key, value) ->
            assertEquals(value, searchPartyNode.findValue(VALUE).findValue(key).asText())
        );

    }

    private void assertSearchCriteriaCreatedButEmpty(Map<String, JsonNode> testData) {

        // ACT
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, testData);

        // ASSERT
        assertNotNull(globalSearchData.get(SEARCH_CRITERIA));
        assertTrue(globalSearchData.get(SEARCH_CRITERIA).isEmpty());

    }

}
