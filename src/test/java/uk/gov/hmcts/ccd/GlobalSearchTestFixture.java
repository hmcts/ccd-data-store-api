package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class GlobalSearchTestFixture {

    private static final String TEST_FIELD_VALUE = "2012-04-21";
    private static final String FIRST_NAME_VALUE = "MyFirstName";
    private static final String LAST_NAME_VALUE = "MyLastName";
    private static final String ADDRESS_LINE_1 = "My Street Address";
    private static final String ADDRESS_LINE_2 = "My Street Address 2";
    private static final String POSTCODE = "SW1H 9AJ";
    private static final String COUNTRY = "GB";

    private GlobalSearchTestFixture() {

    }

    public static Map<String, JsonNode> createCaseData() {
        Map<String, JsonNode> caseData = new HashMap<>();
        try {
            caseData.put("TextField1", JacksonUtils.MAPPER.readTree("\"" + TEST_FIELD_VALUE + "\""));
            caseData.put("PersonFirstName", JacksonUtils.MAPPER.readTree("\"" + FIRST_NAME_VALUE + "\""));
            caseData.put("PersonLastName", JacksonUtils.MAPPER.readTree("\"" + LAST_NAME_VALUE + "\""));
            caseData.put("PersonAddress", JacksonUtils.MAPPER.readTree("{\n"
                + "      \"PostCode\": \"" + POSTCODE + "\",\n"
                + "      \"AddressLine1\": \"" + ADDRESS_LINE_1 + "\",\n"
                + "      \"Country\": \"" + COUNTRY + "\",\n"
                + "      \"AddressLine2\": \"" + ADDRESS_LINE_2 + "\"\n"
                + "}"));
        } catch (JsonProcessingException jpe) {
            fail("Failed to create test case data", jpe);
        }

        return caseData;
    }

    public static void assertGlobalSearchData(Map<String, JsonNode> caseData) {
        JsonNode searchCriteriaJsonNode = caseData.get("SearchCriteria");

        assertEquals(2,
            searchCriteriaJsonNode.get("OtherCaseReferences").size(),
            "Saved case data should contain two SearchCriteria with OtherCaseReferences");

        assertTrue(searchCriteriaJsonNode.get("OtherCaseReferences").findValuesAsText("value")
                .containsAll(List.of(TEST_FIELD_VALUE, ADDRESS_LINE_1)),
            "Saved case data should contain SearchCriteria with OtherCaseReferences");

        assertEquals(3, searchCriteriaJsonNode.get("SearchParties").size());

        List<SearchPartyValue> searchPartyValues = new ArrayList<>();
        searchPartyValues.add(
            SearchPartyValue.builder()
                .name(FIRST_NAME_VALUE + " " + LAST_NAME_VALUE)
                .addressLine1(ADDRESS_LINE_1)
                .postCode(POSTCODE)
                .dateOfBirth(TEST_FIELD_VALUE)
                .build());
        searchPartyValues.add(
            SearchPartyValue.builder()
                .name(FIRST_NAME_VALUE)
                .emailAddress(COUNTRY)
                .addressLine1(ADDRESS_LINE_2)
                .postCode(TEST_FIELD_VALUE)
                .dateOfBirth(TEST_FIELD_VALUE)
                .build());
        searchPartyValues.add(
            SearchPartyValue.builder()
                .name(LAST_NAME_VALUE)
                .addressLine1(ADDRESS_LINE_1)
                .postCode(POSTCODE)
                .dateOfBirth(TEST_FIELD_VALUE)
                .build());

        assertSearchPartyNodesPopulatedAsExpected(searchPartyValues, searchCriteriaJsonNode.get("SearchParties"));
    }


    private static void assertSearchPartyNodesPopulatedAsExpected(List<SearchPartyValue> searchPartyValues,
                                                           JsonNode searchParties) {
        List<SearchPartyValue> searchPartyList = searchParties.findValues("value").stream().map(jsonNode -> {
            try {
                return JacksonUtils.MAPPER.treeToValue(jsonNode, SearchPartyValue.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        Assert.assertTrue(searchPartyList.containsAll(searchPartyValues));
    }

}
