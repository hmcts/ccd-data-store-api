package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.endpoint.std.GlobalSearchEndpoint.GLOBAL_SEARCH_PATH;

public class GlobalSearchEndpointIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    public List<String> listOfValidFields;
    public List<String> listOfInvalidFields;
    public List<SortCriteria> listOfValidSortCriteria;
    public List<SortCriteria> listOfInvalidSortCriteria;

    @Before
    public void setUp() throws IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        listOfValidFields = new ArrayList<>();
        listOfValidFields.add("ValidEntry");
        listOfValidFields.add("ValidEntryTwo");
        listOfInvalidFields = new ArrayList<>();
        listOfInvalidFields.add("INVALID_111111122222223333333444444455555556666666777777788888889999999");
        SortCriteria validSortCriteria = new SortCriteria();
        validSortCriteria.setSortBy(GlobalSearchSortCategory.CASE_NAME.getCategoryName());
        validSortCriteria.setSortDirection(GlobalSearchSortDirection.DESCENDING.name());
        SortCriteria validSortCriteriaTwo = new SortCriteria();
        validSortCriteriaTwo.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());
        validSortCriteriaTwo.setSortBy(GlobalSearchSortCategory.CASE_NAME.getCategoryName());
        listOfValidSortCriteria = new ArrayList<>();
        listOfValidSortCriteria.add(validSortCriteria);
        listOfValidSortCriteria.add(validSortCriteriaTwo);
        SortCriteria invalidSortCriteria = new SortCriteria();
        invalidSortCriteria.setSortBy("invalid");
        invalidSortCriteria.setSortDirection("invalid");
        listOfInvalidSortCriteria = new ArrayList<>();
        listOfInvalidSortCriteria.add(invalidSortCriteria);


    }

    @Test
    public void shouldReturn200WhenRequestDataValid() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(listOfValidSortCriteria);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(listOfValidFields);
        searchCriteria.setCaseManagementRegionIds(listOfValidFields);
        searchCriteria.setCcdCaseTypeIds(listOfValidFields);
        searchCriteria.setCcdJurisdictionIds(listOfValidFields);
        searchCriteria.setOtherReferences(listOfValidFields);
        searchCriteria.setStateIds(listOfValidFields);
        Party party = new Party();
        party.setAddressLine1("address");
        party.setPartyName("name");
        party.setDateOfBirth("1999-01-01");
        party.setPostCode("EC3M 8AF");
        party.setEmailAddress("someone@cgi.com");
        List<Party> list = new ArrayList<>();
        list.add(party);
        Party partyTwo = new Party();
        partyTwo.setAddressLine1("address");
        partyTwo.setPartyName("name");
        partyTwo.setDateOfBirth("1999-01-01");
        partyTwo.setPostCode("EC3M 8AF");
        partyTwo.setEmailAddress("someone@cgi.com");
        list.add(partyTwo);
        searchCriteria.setParties(list);
        payload.setSearchCriteria(searchCriteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    @Test
    public void shouldThrowValidationErrorsWhenDataInvalid() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10001);
        payload.setStartRecordNumber(0);
        payload.setSortCriteria(listOfInvalidSortCriteria);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCcdCaseTypeIds(listOfInvalidFields);
        searchCriteria.setCcdJurisdictionIds(listOfInvalidFields);
        searchCriteria.setCaseReferences(listOfInvalidFields);
        searchCriteria.setStateIds(listOfInvalidFields);
        Party party = new Party();
        party.setAddressLine1("address");
        Party secondParty = new Party();
        secondParty.setAddressLine1("address");
        secondParty.setDateOfBirth("1999-13-01");
        List<Party> list = new ArrayList<>();
        list.add(secondParty);
        list.add(party);
        searchCriteria.setParties(list);
        payload.setSearchCriteria(searchCriteria);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains(ValidationError.PARTIES_INVALID));
        assertTrue(response.contains(ValidationError.SORT_BY_INVALID));
        assertTrue(response.contains(ValidationError.SORT_DIRECTION_INVALID));
        assertTrue(response.contains(ValidationError.MAX_RECORD_COUNT_INVALID));
        assertTrue(response.contains(ValidationError.START_RECORD_NUMBER_INVALID));
        assertTrue(response.contains(ValidationError.JURISDICTION_ID_LENGTH_INVALID));
        assertTrue(response.contains(ValidationError.STATE_ID_LENGTH_INVALID));
        assertTrue(response.contains(ValidationError.CASE_TYPE_ID_LENGTH_INVALID));
        assertTrue(response.contains(ValidationError.DATE_OF_BIRTH_INVALID));
        assertTrue(response.contains(ValidationError.ARGUMENT_INVALID));
        assertTrue(response.contains(ValidationError.CASE_REFERENCE_INVALID));
    }

    @Test
    public void shouldThrowValidationErrorsWhenDataNull() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(listOfValidSortCriteria);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains(ValidationError.ARGUMENT_INVALID));
        assertTrue(response.contains(ValidationError.SEARCH_CRITERIA_MISSING));
    }

    @Test
    public void shouldReturn200WhenEmptyFieldsHaveDefaultValues() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(listOfValidFields);
        searchCriteria.setCaseManagementRegionIds(listOfValidFields);
        payload.setSearchCriteria(searchCriteria);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

}
