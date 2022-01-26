package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.definition.SearchParty;

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

    @BeforeEach
    void setup() throws JsonProcessingException {
        globalSearchProcessorService = new GlobalSearchProcessorService();
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

        // ACT
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, testData);

        // ASSERT
        assertNotNull(globalSearchData.get(SEARCH_CRITERIA));
        assertTrue(globalSearchData.get(SEARCH_CRITERIA).isEmpty());

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
    void checkSearchPartyNamePopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyName("SearchPartyFirstName");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String firstName = "MyFirstNameValue";
        caseData.put("SearchPartyFirstName", JacksonUtils.MAPPER.readTree("\""  + firstName  + "\""));

        assertSearchCriteriaField(NAME, firstName);
    }

    @Test
    void checkSearchPartyNamePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyName("SearchParty.FirstName");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String firstName = "firstNameValue";
        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\"FirstName\": \"" + firstName  + "\"}"));

        assertSearchCriteriaField(NAME, firstName);
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
        final String searchPartyNameFirstName = "John";
        final String searchPartyNameMiddleName = "Johnny";
        final String searchPartyNameLastName = "Johnson";

        final String firstName = "MyPerson.Person.FirstName";
        final String middleName = "MyPerson.Person.MiddleName";
        final String lastName = "MyPerson.Person.LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s", firstName, middleName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("MyPerson", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"Person\":{\n"
            + "         \"FirstName\":\"" + searchPartyNameFirstName + "\",\n"
            + "         \"MiddleName\":\"" + searchPartyNameMiddleName + "\",\n"
            + "         \"LastName\":\"" + searchPartyNameLastName + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));


        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s %s", searchPartyNameFirstName, searchPartyNameMiddleName, searchPartyNameLastName));

        assertSearchCriteriaFields(expectedValues);
    }

    @Test
    void checkCommaSeparatedComplexValuesSearchPartyNameWithNullAndMissingValuesPopulatedInSearchCriteria()
        throws JsonProcessingException {

        final String searchPartyNameFirstName = "John";
        final String searchPartyNameLastName = "Johnson";

        final String firstName = "MyPerson.Person.FirstName";
        final String middleName = "MyPerson.Person.MiddleName"; // test will set this field to null in case data
        final String missingName = "MyPerson.Person.MissingName"; // test will miss this field out of the case data
        final String lastName = "MyPerson.Person.LastName";

        searchParty.setSearchPartyName(String.format("%s,%s,%s,%s", firstName, middleName, missingName, lastName));
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        caseData.put("MyPerson", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"Person\":{\n"
            + "         \"FirstName\":\"" + searchPartyNameFirstName + "\",\n"
            + "         \"MiddleName\": null,\n"
            + "         \"LastName\":\"" + searchPartyNameLastName + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));


        Map<String, String> expectedValues = new HashMap<>();

        expectedValues.put(NAME,
            String.format("%s %s",
                searchPartyNameFirstName,
                searchPartyNameLastName)); // i.e. only the two values returned with no extra spaces for missing values

        assertSearchCriteriaFields(expectedValues);
    }


    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteria() throws JsonProcessingException {
        searchParty.setSearchPartyAddressLine1("SearchPartyAddress");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String address = "MyAddress";
        caseData.put("SearchPartyAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));

        assertSearchCriteriaField(ADDRESS_LINE_1, address);
    }

    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyAddressLine1("SearchParty.Address");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String address = "MyAddress";
        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\"Address\": \"" + address  + "\"}"));

        assertSearchCriteriaField(ADDRESS_LINE_1, address);
    }

    @Test
    void checkSearchPartyEMailAddressPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyEmailAddress("SearchPartyEmailAddress");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String email = "a@b.com";
        caseData.put("SearchPartyEmailAddress", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));

        assertSearchCriteriaField(EMAIL_ADDRESS, email);
    }

    @Test
    void checkSearchPartyEMailAddressPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyEmailAddress("SearchParty.EmailAddress");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String email = "a@b.com";
        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\"EmailAddress\": \"" + email  + "\"}"));

        assertSearchCriteriaField(EMAIL_ADDRESS, email);
    }

    @Test
    void checkSearchPartyPostCodePopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyPostCode("SearchPartyPostCode");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String postCode = "AB1 2CD";
        caseData.put("SearchPartyPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));

        assertSearchCriteriaField(POSTCODE, postCode);
    }

    @Test
    void checkSearchPartyPostCodePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyPostCode("MySearchParty.SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String postCode = "AB1 2CD";

        caseData.put("MySearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "   \"SearchParty\":{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + postCode + "\"\n"
            + "      }\n"
            + "   }\n"
            + "}"));

        assertSearchCriteriaField(POSTCODE, postCode);
    }

    @Test
    void checkSearchPartyDateOfDeathPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchPartyDod");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dod = "1979-05-16";
        caseData.put("SearchPartyDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        assertSearchCriteriaField(DATE_OF_DEATH, dod);
    }

    @Test
    void checkSearchPartyDateOfDeathEmptyInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchPartyDod");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dodField = "FirstName";
        caseData.put("SearchPartyDod", JacksonUtils.MAPPER.readTree("\""  + dodField  + "\""));

        assertSearchCriteriaFieldIsNull();

    }



    @Test
    void checkSearchPartyDateOfDeathPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyDod("SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dod = "1979-05-16";

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + dod + "\"\n"
            + "      }\n"
            + "}"));

        assertSearchCriteriaField(DATE_OF_DEATH, dod);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchPartyDoB");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "1979-05-16";
        caseData.put("SearchPartyDoB", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));

        assertSearchCriteriaField(DATE_OF_BIRTH, dob);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "1979-05-16";

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + dob + "\"\n"
            + "      }\n"
            + "}"));

        assertSearchCriteriaField(DATE_OF_BIRTH, dob);
    }

    @Test
    void checkSearchPartyDateOfBirthEmptyInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchPartyDob");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dobField = "FirstName";
        caseData.put("SearchPartyDob", JacksonUtils.MAPPER.readTree("\""  + dobField  + "\""));

        assertSearchCriteriaFieldIsNull();
    }

    @Test
    void checkSearchCriteriaContainingASearchPartyContainingBotFieldsTwoDateFieldsAreWrong()
        throws JsonProcessingException {

        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyDod("spDod");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "FirstName";
        final String dod = "SecondName";

        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        assertSearchCriteriaFieldIsNull();
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

        final String name =  "name";
        final String dob = "1979-05-16";
        final String dod = "1979-05-16";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, name);
        expectedFieldValues.put(ADDRESS_LINE_1, address);
        expectedFieldValues.put(EMAIL_ADDRESS, email);
        expectedFieldValues.put(POSTCODE, postCode);
        expectedFieldValues.put(DATE_OF_BIRTH, dob);
        expectedFieldValues.put(DATE_OF_DEATH, dod);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

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

        final String name =  "name";
        final String dob = "FirstName";
        final String dod = "SecondName";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, name);
        expectedFieldValues.put(ADDRESS_LINE_1, address);
        expectedFieldValues.put(EMAIL_ADDRESS, email);
        expectedFieldValues.put(POSTCODE, postCode);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNodes = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertEquals(4,searchPartyNodes.findValue(VALUE).size());

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

        final String name =  "name";
        final String dob = "1979-05-16";
        final String dod = "1979-05-16";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        final String name2 =  "another name";
        final String dob2 = "16-05-1980";
        final String dod2 = "16-05-1980";
        final String postCode2 = "EF3 4GH";
        final String email2 = "c@d.com";
        final String address2 = "My 2nd Address";

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + name2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + dob2 + "\"\n"
            + "      },\n"
            + "      \"Dod\":{\n"
            + "         \"value\":\"" + dod2 + "\"\n"
            + "      },\n"
            + "      \"Address\":{\n"
            + "         \"value\":\"" + address2 + "\"\n"
            + "      },\n"
            + "      \"Email\":{\n"
            + "         \"value\":\"" + email2 + "\"\n"
            + "      },\n"
            + "      \"PostCode\":{\n"
            + "         \"value\":\"" + postCode2 + "\"\n"
            + "      }\n"
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
        searchParty2.setSearchPartyName("dont.find.me");
        searchParty2.setSearchPartyDob("dont.find.me");
        searchParty2.setSearchPartyDod("dont.find.me");
        searchParty2.setSearchPartyPostCode("dont.find.me");
        searchParty2.setSearchPartyEmailAddress("dont.find.me");
        searchParty2.setSearchPartyAddressLine1("dont.find.me");

        caseTypeDefinition.setSearchParties(List.of(searchParty, searchParty2));

        final String name =  "name";
        final String dob = "1979-05-16";
        final String dod = "1979-05-16";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        final String name2 =  "another name";
        final String dob2 = "16-05-1980";
        final String dod2 = "16-05-1980";
        final String postCode2 = "EF3 4GH";
        final String email2 = "c@d.com";
        final String address2 = "My 2nd Address";

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, name);
        expectedFieldValues.put(ADDRESS_LINE_1, address);
        expectedFieldValues.put(EMAIL_ADDRESS, email);
        expectedFieldValues.put(POSTCODE, postCode);
        expectedFieldValues.put(DATE_OF_BIRTH, dob);
        expectedFieldValues.put(DATE_OF_DEATH, dod);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));
        caseData.put("spDod", JacksonUtils.MAPPER.readTree("\""  + dod  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + name2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + dob2 + "\"\n"
            + "      },\n"
            + "      \"Dod\":{\n"
            + "         \"value\":\"" + dod2 + "\"\n"
            + "      },\n"
            + "      \"Address\":{\n"
            + "         \"value\":\"" + address2 + "\"\n"
            + "      },\n"
            + "      \"Email\":{\n"
            + "         \"value\":\"" + email2 + "\"\n"
            + "      },\n"
            + "      \"PostCode\":{\n"
            + "         \"value\":\"" + postCode2 + "\"\n"
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

    private void assertSearchCriteriaFieldIsNull() {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNode = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertNull(searchPartyNode);
    }


}
