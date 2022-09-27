package uk.gov.hmcts.ccd.endpoint.std;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_PATH;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_PATH;
import static uk.gov.hmcts.ccd.data.ReferenceDataTestFixtures.BUILDING_LOCATIONS_STUB_ID;
import static uk.gov.hmcts.ccd.data.ReferenceDataTestFixtures.SERVICES_STUB_ID;
import static uk.gov.hmcts.ccd.endpoint.std.GlobalSearchEndpoint.GLOBAL_SEARCH_PATH;

class GlobalSearchEndpointIT extends WireMockBaseTest {

    private static final String ADDRESS_LINE_1 = "address";
    private static final String NAME = "name";
    private static final String DOB  = "1999-01-01";
    private static final String POSTCODE = "EC3M 8AF";
    private static final String EMAIL_ADDRESS = "someone@cgi.com";

    private static final String REFERENCE_1 = "4444333322221111";
    private static final String REFERENCE_2 = "1111222233334444";
    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String STATE = "TODO";
    private static final String SECURITY_CLASSIFICATION = "PUBLIC";

    private static final String SERVICE_ID = "AAA1";
    private static final String SERVICE_NAME = "test_service_short_description"; // see wiremock RefData mappings

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    @MockBean
    private JestClient jestClient;

    @SpyBean
    private AuditRepository auditRepository;

    private List<String> validFields;
    private List<String> invalidFields;
    private List<SortCriteria> validSortCriteria;
    private List<SortCriteria> invalidSortCriteria;
    private List<String> validCaseReferences;
    private static final String DETAILS_FIELD = "$.details";
    private static final String MESSAGE_FIELD = "$.message";

    @BeforeEach
    void setUp() throws IOException {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

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
        validCaseReferences = List.of(REFERENCE_1, "1234-1234-1234-1234", "234-1234*", "1234-1234-1234-123?");

        wireMockServer.resetAll();

        final String buildings = fromFileAsString("tests/refdata/get_building_locations.json");
        final String orgServices = fromFileAsString("tests/refdata/get_org_services.json");

        stubSuccess(BUILDING_LOCATIONS_PATH, buildings, BUILDING_LOCATIONS_STUB_ID);
        stubSuccess(SERVICES_PATH, orgServices, SERVICES_STUB_ID);
    }

    @Test
    void shouldReturn200WhenRequestDataValid() throws Exception {

        // ARRANGE
        stubElasticSearchSearchRequestWillReturn();

        int startRecord = 2;
        GlobalSearchRequestPayload payload = createRequestPayload(startRecord);

        // ACT / ASSERT
        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT extra
        String responseAsString = result.getResponse().getContentAsString();
        GlobalSearchResponsePayload globalSearchResponsePayload = mapper.readValue(responseAsString,
            GlobalSearchResponsePayload.class);

        assertThat(globalSearchResponsePayload.getResultInfo().getCasesReturned(), is(2));
        assertThat(globalSearchResponsePayload.getResultInfo().getCaseStartRecord(), is(startRecord));
        assertThat(globalSearchResponsePayload.getResultInfo().isMoreResultsToGo(), is(true));

        assertThat(globalSearchResponsePayload.getResults().get(0).getCaseReference(), is(REFERENCE_1));
        assertThat(globalSearchResponsePayload.getResults().get(0).getCcdJurisdictionId(), is(JURISDICTION));
        assertThat(globalSearchResponsePayload.getResults().get(0).getStateId(), is(STATE));
        assertThat(globalSearchResponsePayload.getResults().get(0).getCcdCaseTypeId(), is(CASE_TYPE));
        assertThat(globalSearchResponsePayload.getResults().get(0).getHmctsServiceId(), is(SERVICE_ID));
        assertThat(globalSearchResponsePayload.getResults().get(0).getHmctsServiceShortDescription(), is(SERVICE_NAME));
    }

    @Test
    void shouldReturn200WhenEmptyFieldsHaveDefaultValues() throws Exception {

        // ARRANGE
        stubElasticSearchSearchRequestWillReturn();

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria searchCriteria = new SearchCriteria();
        // NB: one of Jurisdiction or CaseType must be supplied
        searchCriteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        searchCriteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        payload.setSearchCriteria(searchCriteria);
        // i.e. leave all fields that will use defaults blank (NB: case-type no longer auto-populated when blank)

        // ACT / ASSERT
        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    @Test
    void shouldThrowValidationErrorsWhenDataInvalid() throws Exception {

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
    void shouldThrowValidationErrorsWhenSearchCriteriaIsNull() throws Exception {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(2);
        payload.setSortCriteria(validSortCriteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.GLOBAL_SEARCH_CRITERIA_INVALID)))
            .andReturn();
    }


    @Test
    void shouldAuditLogSearchCases() throws Exception {

        // ARRANGE
        stubElasticSearchSearchRequestWillReturn();
        GlobalSearchRequestPayload payload = createRequestPayload(2);

        // ACT / ASSERT
        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT extra
        String responseAsString = result.getResponse().getContentAsString();
        GlobalSearchResponsePayload globalSearchResponsePayload = mapper.readValue(responseAsString,
            GlobalSearchResponsePayload.class);

        assertThat(globalSearchResponsePayload.getResults().get(0).getCaseReference(), is(REFERENCE_1));
        assertThat(globalSearchResponsePayload.getResults().get(0).getCcdJurisdictionId(), is(JURISDICTION));
        assertThat(globalSearchResponsePayload.getResults().get(0).getStateId(), is(STATE));
        assertThat(globalSearchResponsePayload.getResults().get(0).getCcdCaseTypeId(), is(CASE_TYPE));
        assertThat(globalSearchResponsePayload.getResults().get(0).getHmctsServiceId(), is(SERVICE_ID));
        assertThat(globalSearchResponsePayload.getResults().get(0).getHmctsServiceShortDescription(), is(SERVICE_NAME));

        // check auditing
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.GLOBAL_SEARCH.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(REFERENCE_1 + "," + REFERENCE_2));
        assertThat(captor.getValue().getIdamId(), is("123"));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getListOfCaseTypes(), is("TestAddressBookCase"));
    }

    @Test
    void shouldThrowValidationErrorsWhenSearchCriteriaIsEmpty() throws Exception {

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
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.GLOBAL_SEARCH_CRITERIA_INVALID)))
            .andReturn();
    }

    @ParameterizedTest(
        name = "Should throw ValidationError when Jurisdiction and CaseType criteria fields are null or empty: {0}"
    )
    @NullAndEmptySource
    void shouldThrowValidationErrorsWhenSearchCriteriaJurisdictionAndCaseTypeAreNullOrEmpty(List<String> values)
        throws Exception {

        GlobalSearchRequestPayload payload = createRequestPayload(2);

        SearchCriteria criteria = payload.getSearchCriteria();
        criteria.setCcdJurisdictionIds(values);
        criteria.setCcdCaseTypeIds(values);
        payload.setSearchCriteria(new SearchCriteria());

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(400))
            .andExpect(jsonPath(MESSAGE_FIELD, is(ValidationError.ARGUMENT_INVALID)))
            .andExpect(jsonPath(DETAILS_FIELD, hasItem(ValidationError.GLOBAL_SEARCH_CRITERIA_INVALID)))
            .andReturn();
    }

    @Test
    void shouldReturn200WhenOneValidFieldInSearchCriteria_CaseType() throws Exception {

        // ARRANGE
        stubElasticSearchSearchRequestWillReturn();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        List<String> emptyList = new ArrayList<>();
        criteria.setCcdJurisdictionIds(emptyList);
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);

        // ACT / ASSERT
        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    @Test
    void shouldReturn200WhenOneValidFieldInSearchCriteria_Jurisdiction() throws Exception {

        // ARRANGE
        stubElasticSearchSearchRequestWillReturn();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(null);
        criteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);

        // ACT / ASSERT
        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    private String create2CaseDetailsElastic(String caseDetails1, String caseDetails2) {
        return "{\n"
            + "   \"took\":177,\n"
            + "   \"hits\":{\n"
            + "      \"total\": 30,"
            + "      \"hits\":[\n"
            + "         {\n"
            + "            \"_index\":\"TestAddressBookCase_cases-000001\",\n"
            + "            \"_source\":" + createCaseDetails(caseDetails1)
            + "         },\n"
            + "         {\n"
            + "            \"_index\":\"TestAddressBookCase_cases-000001\",\n"
            + "            \"_source\":" + createCaseDetails(caseDetails2)
            + "         }\n"
            + "      ]\n"
            + "   }\n"
            + "}";
    }

    private String createCaseDetails(String reference) {
        return "{\n"
            + "\"id\": 18,\n"
            + "\"" + GlobalSearchFields.REFERENCE + "\": \"" + reference + "\",\n"
            + "\"" + GlobalSearchFields.JURISDICTION + "\": \"" + JURISDICTION + "\",\n"
            + "\"" + GlobalSearchFields.CASE_TYPE + "\": \"" + CASE_TYPE + "\",\n"
            + "\"" + GlobalSearchFields.STATE + "\": \"" + STATE + "\",\n"
            + "\"" + GlobalSearchFields.SECURITY_CLASSIFICATION + "\": \"" + SECURITY_CLASSIFICATION + "\",\n"
            + "\"" + GlobalSearchFields.CREATED_DATE + "\": \"2021-09-07T13:38:00.050Z\",\n"
            + "\"last_state_modified_date\": \"2021-09-07T13:38:00.050Z\",\n"
            + "\"last_modified\": \"2021-09-07T13:38:00.062Z\","
            + "\"data\": {},\n"
            + "\"supplementary_data\": {\n"
            + "    \"" + GlobalSearchFields.SupplementaryDataFields.SERVICE_ID + "\": \"" + SERVICE_ID + "\"\n"
            + "  }\n"
            + "}";
    }

    private void stubElasticSearchSearchRequestWillReturn() throws java.io.IOException {
        String caseDetailElastic = create2CaseDetailsElastic(REFERENCE_1, REFERENCE_2);

        Gson gson = new Gson();
        JsonObject convertedObject = gson.fromJson(caseDetailElastic, JsonObject.class);
        MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
        when(multiSearchResult.isSucceeded()).thenReturn(true);

        SearchResult searchResult = new SearchResult(gson);
        searchResult.setSucceeded(true);
        searchResult.setJsonObject(convertedObject);
        searchResult.setJsonString(convertedObject.toString());
        searchResult.setPathToResult("hits/hits/_source");

        MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
        when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
        Whitebox.setInternalState(response, "searchResult", searchResult);

        given(jestClient.execute(any())).willReturn(multiSearchResult);
    }


    private GlobalSearchRequestPayload createRequestPayload(int startRecord) {
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(10);
        payload.setStartRecordNumber(startRecord);
        payload.setSortCriteria(validSortCriteria);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
        searchCriteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        searchCriteria.setCcdJurisdictionIds(validFields);
        searchCriteria.setOtherReferences(validFields);
        searchCriteria.setStateIds(validFields);
        searchCriteria.setCaseReferences(validCaseReferences);
        Party party = new Party();
        party.setAddressLine1(ADDRESS_LINE_1);
        party.setPartyName(NAME);
        party.setDateOfBirth(DOB);
        party.setPostCode(POSTCODE);
        party.setEmailAddress(EMAIL_ADDRESS);
        Party partyTwo = new Party();
        partyTwo.setAddressLine1(ADDRESS_LINE_1);
        partyTwo.setPartyName(NAME);
        partyTwo.setDateOfBirth(DOB);
        partyTwo.setPostCode(POSTCODE);
        partyTwo.setEmailAddress(EMAIL_ADDRESS);
        List<Party> list = List.of(party, partyTwo);
        searchCriteria.setParties(list);
        payload.setSearchCriteria(searchCriteria);
        return payload;
    }

}
