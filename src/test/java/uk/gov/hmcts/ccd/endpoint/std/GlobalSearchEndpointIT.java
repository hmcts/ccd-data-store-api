package uk.gov.hmcts.ccd.endpoint.std;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final String REFERENCE_1 = "4444333322221111";
    private static final String REFERENCE_2 = "1111222233334444";
    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String STATE = "TODO";

    private static final String SERVICE_ID = "AAA1";
    private static final String SERVICE_NAME = "test_service_short_description"; // see wiremock RefData mappings

    private static final String JSON_CONTENT_TYPE = "application/json";

    private MockMvc mockMvc;

    @MockitoBean
    private ElasticsearchClient elasticsearchClient;

    @MockitoSpyBean
    private AuditRepository auditRepository;

    private List<String> validFields;
    private List<String> invalidFields;
    private List<SortCriteria> validSortCriteria;
    private List<SortCriteria> invalidSortCriteria;
    private List<String> validCaseReferences;
    private static final String DETAILS_FIELD = "$.details";
    private static final String MESSAGE_FIELD = "$.message";

    @BeforeEach
    void setup(WebApplicationContext wac) {
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
        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(mockMultiSearchResponse());

        GlobalSearchRequestPayload payload = createRequestPayload(2);

        MvcResult result = mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().isOk())
            .andReturn();

        GlobalSearchResponsePayload response = mapper.readValue(
            result.getResponse().getContentAsString(),
            GlobalSearchResponsePayload.class
        );

        assertThat(response.getResultInfo().getCasesReturned(), is(2));
        assertThat(response.getResultInfo().getCaseStartRecord(), is(2));
        assertThat(response.getResultInfo().isMoreResultsToGo(), is(true));
        assertThat(response.getResults(), hasSize(2));
        assertThat(response.getResults().get(0).getCaseReference(), is(REFERENCE_1));
    }

    @Test
    void shouldReturn200WhenEmptyFieldsHaveDefaultValues() throws Exception {
        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(mockMultiSearchResponse());

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
        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(mockMultiSearchResponse());

        GlobalSearchRequestPayload payload = createRequestPayload(2);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().isOk());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getCaseId(), is(REFERENCE_1 + "," + REFERENCE_2));
        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.GLOBAL_SEARCH.getLabel()));
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

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturn400IfJurisdictionAndCaseTypeAreEmpty(List<String> empty) throws Exception {
        GlobalSearchRequestPayload payload = createRequestPayload(2);
        payload.getSearchCriteria().setCcdJurisdictionIds(empty);
        payload.getSearchCriteria().setCcdCaseTypeIds(empty);

        mockMvc.perform(post(GLOBAL_SEARCH_PATH)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is(ValidationError.ARGUMENT_INVALID)));
    }

    private GlobalSearchRequestPayload createRequestPayload(int startRecord) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCcdJurisdictionIds(List.of(JURISDICTION));
        criteria.setCcdCaseTypeIds(List.of(CASE_TYPE));
        criteria.setCaseReferences(List.of(REFERENCE_1, REFERENCE_2));

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setSearchCriteria(criteria);
        payload.setStartRecordNumber(startRecord);
        payload.setMaxReturnRecordCount(10);
        payload.setSortCriteria(validSortCriteria);

        return payload;
    }

    @Test
    void shouldReturn200WhenOneValidFieldInSearchCriteria_CaseType() throws Exception {

        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(mockMultiSearchResponse());

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
        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(mockMultiSearchResponse());

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

    private JsonNode toJsonNode(String value) {
        return new ObjectMapper().convertValue(value, JsonNode.class);
    }

    private MsearchResponse<ElasticSearchCaseDetailsDTO> mockMultiSearchResponse() {
        final String caseId1 = "000001";
        final String caseId2 = "000002";
        ElasticSearchCaseDetailsDTO dto1 = createElasticSearchCaseDetailsDTO(caseId1, REFERENCE_1);
        ElasticSearchCaseDetailsDTO dto2 = createElasticSearchCaseDetailsDTO(caseId2, REFERENCE_2);

        // Build hits with required fields
        Hit<ElasticSearchCaseDetailsDTO> hit1 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .id(dto1.getId())
            .index(CASE_TYPE)
            .source(dto1)
            .build();

        Hit<ElasticSearchCaseDetailsDTO> hit2 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .id(dto2.getId())
            .index(CASE_TYPE)
            .source(dto2)
            .build();

        HitsMetadata<ElasticSearchCaseDetailsDTO> hitsMetadata = new HitsMetadata.Builder<ElasticSearchCaseDetailsDTO>()
            .hits(List.of(hit1, hit2))
            .total(new TotalHits.Builder()
                .relation(TotalHitsRelation.Eq)
                .value(30).build())
            .build();

        // Wrap in MultiSearch response
        MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem
            = new MultiSearchResponseItem.Builder<ElasticSearchCaseDetailsDTO>()
            .result(r -> r
                .took(123)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
                .hits(hitsMetadata)
            ).build();

        return new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
            .responses(List.of(responseItem))
            .took(123)
            .build();
    }

    private ElasticSearchCaseDetailsDTO createElasticSearchCaseDetailsDTO(String id, String reference) {
        ElasticSearchCaseDetailsDTO dto = new ElasticSearchCaseDetailsDTO();
        dto.setId(id);
        dto.setReference(reference);
        dto.setJurisdiction(JURISDICTION);
        dto.setCaseTypeId(CASE_TYPE);
        dto.setState(STATE);
        dto.setCreatedDate(LocalDateTime.now());
        dto.setLastModified(LocalDateTime.now());
        dto.setLastStateModifiedDate(LocalDateTime.now());
        dto.setSecurityClassification(SecurityClassification.PUBLIC);
        dto.setDataClassification(Collections.emptyMap());
        dto.setData(Map.of("hmctsServiceShortDescription",
            objectMapper.convertValue(SERVICE_NAME, JsonNode.class)));
        dto.setSupplementaryData(Map.of("hmctsServiceId", objectMapper.convertValue(SERVICE_ID, JsonNode.class)));

        return dto;
    }
}
