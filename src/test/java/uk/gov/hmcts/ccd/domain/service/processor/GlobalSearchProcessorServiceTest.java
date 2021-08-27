package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.definition.SearchParty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalSearchProcessorServiceTest {

    private GlobalSearchProcessorService globalSearchProcessorService;

    private static CaseTypeDefinition caseTypeDefinition;

    private Map<String, JsonNode> caseData;

    private List<String> searchPartyPropertyNames;

    private SearchParty searchParty;

    private static final String SEARCH_CRITERIA = "SearchCriteria";
    private static final String SEARCH_PARTIES = "SearchParties";

    private static final String NAME = "Name";
    private static final String ADDRESS_LINE = "AddressLine1";
    private static final String EMAIL_ADDRESS = "EmailAddress";
    private static final String POST_CODE = "PostCode";
    private static final String DATE_OF_BIRTH = "DateOfBirth";

    @BeforeEach
    void setup() throws JsonProcessingException {
        globalSearchProcessorService = new GlobalSearchProcessorService();
        caseTypeDefinition = new CaseTypeDefinition();

        caseData = new HashMap<>();
        caseData.put("PersonFirstName", JacksonUtils.MAPPER.readTree("\"FirstNameValue\""));

        searchPartyPropertyNames = List.of(NAME, ADDRESS_LINE, EMAIL_ADDRESS, POST_CODE, DATE_OF_BIRTH);
        searchParty = new SearchParty();
    }

    @Test
    void checkNoMatchingCaseDataResultsInNoSearchCriteriaCreation() {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);
        assertNull(globalSearchData.get(SEARCH_CRITERIA));
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
        assertEquals(lastName, searchCriteriaNode.get("OtherCaseReferences").findValue("value").asText());
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.findValue("id").asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get("OtherCaseReferences").findValue("id").asText()));
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
            searchCriteriaNode.findValue("id").asText()));
        assertDoesNotThrow(() -> UUID.fromString(
            searchCriteriaNode.get("OtherCaseReferences").findValue("id").asText()));
        assertEquals(addressValue, searchCriteriaNode.get("OtherCaseReferences").findValue("value").asText());
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
    @Disabled
    void checkCommaSeparatedValuesSearchPartyNamePopulatedInSearchCriteria() throws JsonProcessingException {
        final String addressValue = "This is my address";

        //caseData.put("PersonAddress.AddressLine1", JacksonUtils.MAPPER.readTree("\""  + addressValue  + "\""));
        //TextFieldFName,TextFieldMName,TextFieldLName
        caseData.put("PersonAddress", JacksonUtils.MAPPER.readTree("{\"AddressLine1\": \"" + addressValue  + "\"}"));

        final String lastName = "LastNameValue";
        caseData.put("TextField1", JacksonUtils.MAPPER.readTree("\""  + lastName  + "\""));

        assertSearchCriteriaField("SearchPartyName", addressValue);
    }


    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteria() throws JsonProcessingException {
        searchParty.setSearchPartyAddressLine1("SearchPartyAddress");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String address = "MyAddress";
        caseData.put("SearchPartyAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));

        assertSearchCriteriaField(ADDRESS_LINE, address);
    }

    @Test
    void checkSearchPartyAddressLinePopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyAddressLine1("SearchParty.Address");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String address = "MyAddress";
        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\"Address\": \"" + address  + "\"}"));

        assertSearchCriteriaField(ADDRESS_LINE, address);
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

        assertSearchCriteriaField(POST_CODE, postCode);
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

        assertSearchCriteriaField(POST_CODE, postCode);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteria() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchPartyDoB");
        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "16-05-1979";
        caseData.put("SearchPartyDoB", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));

        assertSearchCriteriaField(DATE_OF_BIRTH, dob);
    }

    @Test
    void checkSearchPartyDateOfBirthPopulatedInSearchCriteriaWithComplexField() throws JsonProcessingException {

        searchParty.setSearchPartyDob("SearchParty.PostCode.name");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String dob = "16-05-1979";

        caseData.put("SearchParty", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\":{\n"
            + "         \"name\":\"" + dob + "\"\n"
            + "      }\n"
            + "}"));

        assertSearchCriteriaField(DATE_OF_BIRTH, dob);
    }

    @Test
    void checkSearchCriteriaContainingASeachPartyContainingAllFields() throws JsonProcessingException {

        searchParty.setSearchPartyName("spName");
        searchParty.setSearchPartyDob("spDob");
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        caseTypeDefinition.setSearchParties(List.of(searchParty));

        final String name =  "name";
        final String dob = "16-05-1979";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, name);
        expectedFieldValues.put(ADDRESS_LINE, address);
        expectedFieldValues.put(EMAIL_ADDRESS, email);
        expectedFieldValues.put(POST_CODE, postCode);
        expectedFieldValues.put(DATE_OF_BIRTH, dob);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));

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
        final String dob = "16-05-1979";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        final String name2 =  "another name";
        final String dob2 = "16-05-1980";
        final String postCode2 = "EF3 4GH";
        final String email2 = "c@d.com";
        final String address2 = "My 2nd Address";

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + name2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + dob2 + "\"\n"
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
        searchParty.setSearchPartyPostCode("spPostCode");
        searchParty.setSearchPartyEmailAddress("spEmail");
        searchParty.setSearchPartyAddressLine1("spAddress");

        SearchParty searchParty2 = new SearchParty();
        searchParty2.setSearchPartyName("dont.find.me");
        searchParty2.setSearchPartyDob("dont.find.me");
        searchParty2.setSearchPartyPostCode("dont.find.me");
        searchParty2.setSearchPartyEmailAddress("dont.find.me");
        searchParty2.setSearchPartyAddressLine1("dont.find.me");

        caseTypeDefinition.setSearchParties(List.of(searchParty, searchParty2));

        final String name =  "name";
        final String dob = "16-05-1979";
        final String postCode = "AB1 2CD";
        final String email = "a@b.com";
        final String address = "My Address";

        final String name2 =  "another name";
        final String dob2 = "16-05-1980";
        final String postCode2 = "EF3 4GH";
        final String email2 = "c@d.com";
        final String address2 = "My 2nd Address";

        Map<String, String> expectedFieldValues = new HashMap<>();

        expectedFieldValues.put(NAME, name);
        expectedFieldValues.put(ADDRESS_LINE, address);
        expectedFieldValues.put(EMAIL_ADDRESS, email);
        expectedFieldValues.put(POST_CODE, postCode);
        expectedFieldValues.put(DATE_OF_BIRTH, dob);

        caseData.put("spName", JacksonUtils.MAPPER.readTree("\""  + name  + "\""));
        caseData.put("spAddress", JacksonUtils.MAPPER.readTree("\""  + address  + "\""));
        caseData.put("spPostCode", JacksonUtils.MAPPER.readTree("\""  + postCode  + "\""));
        caseData.put("spEmail", JacksonUtils.MAPPER.readTree("\""  + email  + "\""));
        caseData.put("spDob", JacksonUtils.MAPPER.readTree("\""  + dob  + "\""));

        caseData.put("sp", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"Name\":{\n"
            + "         \"value\":\"" + name2 + "\"\n"
            + "      },\n"
            + "      \"Dob\":{\n"
            + "         \"value\":\"" + dob2 + "\"\n"
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
            assertEquals(value, searchPartyNode.findValue("value").findValue(key).asText()));
    }

    private void assertSearchCriteriaField(String fieldName, Object expectedValue) {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNode = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertDoesNotThrow(() -> UUID.fromString(searchPartyNode.findValue("id").asText()));
        assertEquals(expectedValue, searchPartyNode.findValue("value").findValue(fieldName).asText());

        searchPartyPropertyNames.stream()
            .filter(propertyName -> !propertyName.equals(fieldName))
            .forEach(field -> assertFalse(searchPartyNode.findValue("value").has(field)));
    }

    private void assertSearchCriteriaFields(Map<String, String> expectedValues) {
        Map<String, JsonNode> globalSearchData =
            globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, caseData);

        JsonNode searchPartyNode = globalSearchData.get(SEARCH_CRITERIA).get(SEARCH_PARTIES);

        assertDoesNotThrow(() -> UUID.fromString(searchPartyNode.findValue("id").asText()));

        expectedValues.forEach((key, value) ->
            assertEquals(value, searchPartyNode.findValue("value").findValue(key).asText())
        );
    }
}
