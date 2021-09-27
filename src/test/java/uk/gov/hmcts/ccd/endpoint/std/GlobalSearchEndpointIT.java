package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.endpoint.std.GlobalSearchEndpoint.GLOBAL_SEARCH_PATH;

public class GlobalSearchEndpointIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    private List<String> validFields;
    private List<String> invalidFields;
    private List<SortCriteria> validSortCriteria;
    private List<SortCriteria> invalidSortCriteria;
    private List<String> validCaseReferences;
    private static final String DETAILS_FIELD = "$.details";
    private static final String MESSAGE_FIELD = "$.message";

    @Before
    public void setUp() throws IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        validFields = List.of("ValidEntry", "ValidEntryTwo");
        invalidFields = List.of("INVALID_111111122222223333333444444455555556666666777777788888889999999");
        SortCriteria validSortCriteriaOne = new SortCriteria();
        validSortCriteriaOne.setSortBy(GlobalSearchSortByCategory.CASE_NAME.getCategoryName());
        validSortCriteriaOne.setSortDirection(GlobalSearchSortDirection.DESCENDING.name());
        SortCriteria validSortCriteriaTwo = new SortCriteria();
        validSortCriteriaTwo.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());
        validSortCriteriaTwo.setSortBy(GlobalSearchSortByCategory.CASE_NAME.getCategoryName());
        validSortCriteria = List.of(validSortCriteriaOne, validSortCriteriaTwo);
        SortCriteria invalidSortCriteriaOne = new SortCriteria();
        invalidSortCriteriaOne.setSortBy("invalid");
        invalidSortCriteriaOne.setSortDirection("invalid");
        invalidSortCriteria = List.of(invalidSortCriteriaOne);
        validCaseReferences = List.of("1234123412341234", "1234-1234-1234-1234", "234-1234*", "1234-1234-1234-123?");
    }

    @Test
    public void shouldReturn200WhenRequestDataValid() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(validSortCriteria);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
        searchCriteria.setCcdCaseTypeIds(validFields);
        searchCriteria.setCcdJurisdictionIds(validFields);
        searchCriteria.setOtherReferences(validFields);
        searchCriteria.setStateIds(validFields);
        searchCriteria.setCaseReferences(validCaseReferences);
        Party party = new Party();
        party.setAddressLine1("address");
        party.setPartyName("name");
        party.setDateOfBirth("1999-01-01");
        party.setPostCode("EC3M 8AF");
        party.setEmailAddress("someone@cgi.com");
        Party partyTwo = new Party();
        partyTwo.setAddressLine1("address");
        partyTwo.setPartyName("name");
        partyTwo.setDateOfBirth("1999-01-01");
        partyTwo.setPostCode("EC3M 8AF");
        partyTwo.setEmailAddress("someone@cgi.com");
        List<Party> list = List.of(party, partyTwo);
        searchCriteria.setParties(list);
        payload.setSearchCriteria(searchCriteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    @Test
    public void shouldReturn200WhenEmptyFieldsHaveDefaultValues() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
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
        payload.setSortCriteria(invalidSortCriteria);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCcdCaseTypeIds(invalidFields);
        searchCriteria.setCcdJurisdictionIds(invalidFields);
        searchCriteria.setCaseReferences(invalidFields);
        searchCriteria.setStateIds(invalidFields);
        Party party = new Party();
        party.setAddressLine1("address");
        party.setDateOfBirth("1999-13-01");
        party.setDateOfDeath("2026-12-32");
        List<Party> list = List.of(party);
        searchCriteria.setParties(list);
        payload.setSearchCriteria(searchCriteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.DATE_OF_DEATH_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.SORT_BY_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.SORT_DIRECTION_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.MAX_RECORD_COUNT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.START_RECORD_NUMBER_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.JURISDICTION_ID_LENGTH_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.STATE_ID_LENGTH_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.CASE_TYPE_ID_LENGTH_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.DATE_OF_BIRTH_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.CASE_REFERENCE_INVALID)))
            .andReturn();
    }

    @Test
    public void shouldThrowValidationErrorsWhenSearchCriteriaNull() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(validSortCriteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.SEARCH_CRITERIA_MISSING)))
            .andReturn();
    }

    @Test
    public void shouldThrowValidationErrorsWhenSearchCriteriaEmpty() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(validSortCriteria);
        payload.setSearchCriteria(new SearchCriteria());

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.SEARCH_CRITERIA_MISSING)))
            .andReturn();
    }

    @Test
    public void shouldThrowValidationErrorsWhenSearchCriteriaPartiesIsEmpty() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria criteria = new SearchCriteria();
        List<Party> partyList = new ArrayList<>();
        partyList.add(new Party());
        criteria.setParties(partyList);
        payload.setSearchCriteria(new SearchCriteria());

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.SEARCH_CRITERIA_MISSING)))
            .andReturn();
    }

    @Test
    public void shouldReturn200WhenOneValidFieldInSearchCriteria() throws Exception {

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(null);
        List<String> emptyList = new ArrayList<>();
        criteria.setCcdJurisdictionIds(emptyList);
        List<Party> partyList = new ArrayList<>();
        partyList.add(new Party());
        Party validParty = new Party();
        validParty.setPartyName("name");
        partyList.add(validParty);
        criteria.setParties(partyList);
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }
}
