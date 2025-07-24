package uk.gov.hmcts.ccd.endpoint.std;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.com.google.common.base.Predicate;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.global.*;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

    private static final String JSON_CONTENT_TYPE = "application/json";

    private MockMvc mockMvc;

    @MockitoBean
    private CaseSearchRequest caseSearchRequest;

    @MockitoBean
    private ElasticsearchClient elasticsearchClient;

    @MockitoSpyBean
    private AuditRepository auditRepository;

    @MockitoBean
    private CaseDefinitionRepository caseDefinitionRepository;

    @MockitoBean
    private AuthorisedCaseDefinitionDataService caseDefinitionDataService;

    @MockitoBean
    private CaseTypeDefinition caseTypeDefinition;

    private List<String> validFields;
    private List<String> invalidFields;
    private List<SortCriteria> validSortCriteria;
    private List<SortCriteria> invalidSortCriteria;
    private List<String> validCaseReferences;
    private static final String DETAILS_FIELD = "$.details";
    private static final String MESSAGE_FIELD = "$.message";

    @BeforeEach
    void setUp(WebApplicationContext wac) {
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
        stubElasticSearchSearchRequestWillReturn();

        GlobalSearchRequestPayload payload = createRequestPayload(2);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        GlobalSearchResponsePayload response = mapper.readValue(responseAsString, GlobalSearchResponsePayload.class);

        assertThat(response.getResults().get(0).getCaseReference(), is(REFERENCE_1));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getCaseId(), is(REFERENCE_1 + "," + REFERENCE_2));
    }

    @Test
    void shouldReturn200WhenEmptyFieldsHaveDefaultValues() throws Exception {
        stubElasticSearchSearchRequestWillReturn();

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria searchCriteria = new SearchCriteria();
        // NB: one of Jurisdiction or CaseType must be supplied
        searchCriteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        searchCriteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        payload.setSearchCriteria(searchCriteria);
        // i.e. leave all fields that will use defaults blank (NB: case-type no longer auto-populated when blank)

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

        stubElasticSearchSearchRequestWillReturn();
        GlobalSearchRequestPayload payload = createRequestPayload(2);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();

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

        stubElasticSearchSearchRequestWillReturn();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        List<String> emptyList = new ArrayList<>();
        criteria.setCcdJurisdictionIds(emptyList);
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    @Test
    void shouldReturn200WhenOneValidFieldInSearchCriteria_Jurisdiction() throws Exception {

        stubElasticSearchSearchRequestWillReturn();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdCaseTypeIds(null);
        criteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().is(200))
            .andReturn();
    }

    private void stubElasticSearchSearchRequestWillReturn() throws Exception {
        when(caseDefinitionRepository.getCaseType(anyString()))
            .thenReturn(caseTypeDefinition);

        when(caseDefinitionDataService.getAuthorisedCaseType(anyString(), any(Predicate.class)))
            .thenReturn(Optional.of(caseTypeDefinition));

        when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE);
        //when(caseTypeDefinition.getJurisdiction()).thenReturn(JURISDICTION);
        //when(caseTypeDefinition.getSecurityClassification())
        //    .thenReturn(SecurityClassification.valueOf(SECURITY_CLASSIFICATION));
        //when(caseTypeDefinition.getHmctsServiceId()).thenReturn(SERVICE_ID);
        //when(caseTypeDefinition.getHmctsServiceShortDescription()).thenReturn(SERVICE_NAME);

        ElasticSearchCaseDetailsDTO dto1 = new ElasticSearchCaseDetailsDTO();
        dto1.setId("dummy-id");
        dto1.setReference("dummy-ref");
        dto1.setJurisdiction("dummy-jurisdiction");
        dto1.setCaseTypeId("dummy-case-type");
        dto1.setCreatedDate(LocalDateTime.now());
        dto1.setLastModified(LocalDateTime.now());
        dto1.setLastStateModifiedDate(LocalDateTime.now());
        dto1.setState("dummy-state");
        dto1.setSecurityClassification(SecurityClassification.PUBLIC); // or any valid enum value
        dto1.setData(Collections.emptyMap());
        dto1.setDataClassification(Collections.emptyMap());
        dto1.setSupplementaryData(Collections.emptyMap());

        ElasticSearchCaseDetailsDTO dto2 = new ElasticSearchCaseDetailsDTO();
        dto2.setId("dummy-id");
        dto2.setReference("dummy-ref");
        dto2.setJurisdiction("dummy-jurisdiction");
        dto2.setCaseTypeId("dummy-case-type");
        dto2.setCreatedDate(LocalDateTime.now());
        dto2.setLastModified(LocalDateTime.now());
        dto2.setLastStateModifiedDate(LocalDateTime.now());
        dto2.setState("dummy-state");
        dto2.setSecurityClassification(SecurityClassification.PUBLIC); // or any valid enum value
        dto2.setData(Collections.emptyMap());
        dto2.setDataClassification(Collections.emptyMap());
        dto2.setSupplementaryData(Collections.emptyMap());

        Hit<ElasticSearchCaseDetailsDTO> hit1 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .index("TestAddressBookCase_cases-000001").source(dto1).build();
        Hit<ElasticSearchCaseDetailsDTO> hit2 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .index("TestAddressBookCase_cases-000001").source(dto2).build();

        MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> item
            = new MultiSearchResponseItem.Builder<ElasticSearchCaseDetailsDTO>()
            .result(r -> r
                .took(123)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
                .hits(
                h -> h.hits(List.of(hit1, hit2))
                    .total(t -> t.value(30L).relation(TotalHitsRelation.Eq))))
            .build();

        MsearchResponse<ElasticSearchCaseDetailsDTO> response = new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
            .responses(List.of(item))
            .took(123)
            .build();

        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(response);
    }

    private GlobalSearchRequestPayload createRequestPayload(int startRecord) {
        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setStartRecordNumber(startRecord);
        payload.setMaxReturnRecordCount(10);
        payload.setSortCriteria(validSortCriteria);

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        criteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        payload.setSearchCriteria(criteria);

        return payload;
    }
}
