package uk.gov.hmcts.ccd.endpoint.std;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CaseSearchEndpointIT extends WireMockBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @MockitoBean
    private ElasticsearchClient elasticsearchClient;

    @MockitoSpyBean
    private AuditRepository auditRepository;

    @BeforeEach
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void testSearchCaseDetails() throws Exception {

        final long referenceId = 1535450291607660L;
        String caseDetailElastic = create1CaseDetailsElastic(referenceId);
        stubElasticSearchSearchRequestWillReturn(caseDetailElastic);

        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(JSON_CONTENT_TYPE)
                .param("ctid", "TestAddressBookCase")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
                                                              CaseSearchResult.class);

        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(1));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(referenceId))));
        assertThat(caseDetails, hasItem(hasProperty("jurisdiction", equalTo("PROBATE"))));
        assertThat(caseDetails, hasItem(hasProperty("caseTypeId",
            equalTo("TestAddressBookCase"))));
        assertThat(caseDetails, hasItem(hasProperty("lastModified",
                                                    equalTo(LocalDateTime.parse("2018-08-28T09:58:11.643")))));
        assertThat(caseDetails, hasItem(hasProperty("createdDate",
                equalTo(LocalDateTime.parse("2018-08-28T09:58:11.627")))));
        assertThat(caseDetails, hasItem(hasProperty("state", equalTo("TODO"))));
        assertThat(caseDetails, hasItem(hasProperty("securityClassification",
                equalTo(SecurityClassification.PUBLIC))));
    }

    @Test
    void shouldAuditLogSearchCases() throws Exception {

        final long reference1 = 1535450291607660L;
        final long reference2 = 1535450291607670L;
        String caseDetailElastic1 = create2CaseDetailsElastic(reference1, reference2);

        stubElasticSearchSearchRequestWillReturn(caseDetailElastic1);

        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("ctid", "TestAddressBookCase", "TestAddressBookCase4")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
            CaseSearchResult.class);

        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607660L))));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607670L))));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1535450291607660,1535450291607670"));
        assertThat(captor.getValue().getIdamId(), is("123"));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getListOfCaseTypes(), is("TestAddressBookCase,TestAddressBookCase4"));
    }

    private String create1CaseDetailsElastic(Long reference) {
        return "{\n" +
            "   \"took\":177,\n" +
            "   \"hits\":{\n" +
            "      \"total\": 2," +
            "      \"hits\":[\n" +
            "         {\n" +
            "            \"_index\":\"TestAddressBookCase_cases-000001\",\n" +
            "            \"_source\":" + createCaseDetails(reference) +
            "         }\n" +
            "      ]\n" +
            "   }\n" +
            "}";
    }

    private String create2CaseDetailsElastic(Long reference1,Long reference2) {
        return "{\n" +
            "   \"took\":177,\n" +
            "   \"hits\":{\n" +
            "      \"total\": 2," +
            "      \"hits\":[\n" +
            "         {\n" +
            "            \"_index\":\"TestAddressBookCase_cases-000001\",\n" +
            "            \"_source\":" + createCaseDetails(reference1) +
            "         },\n" +
            "         {\n" +
            "            \"_index\":\"TestAddressBookCase_cases-000001\",\n" +
            "            \"_source\":" + createCaseDetails(reference2) +
            "         }\n" +
            "      ]\n" +
            "   }\n" +
            "}";
    }

    private String createCaseDetails(Long reference) {
        return "{\n"
            + "\"reference\": " + reference + ",\n"
            + "\"last_modified\": \"2018-08-28T09:58:11.643Z\",\n"
            + "\"state\": \"TODO\",\n"
            + "\"@version\": \"1\",\n"
            + "\"data_classification\": {},\n"
            + "\"id\": 18,\n"
            + "\"security_classification\": \"PUBLIC\",\n"
            + "\"jurisdiction\": \"PROBATE\",\n"
            + "\"@timestamp\": \"2018-08-28T09:58:13.044Z\",\n"
            + "\"data\": {},\n"
            + "\"created_date\": \"2018-08-28T09:58:11.627Z\",\n"
            + "\"index_id\": \"probate_aat_cases\",\n"
            + "\"case_type_id\": \"TestAddressBookCase\"\n"
            + "}";
    }

    private void stubElasticSearchSearchRequestWillReturn(String caseDetailElastic) throws Exception {
        List<ElasticSearchCaseDetailsDTO> caseDetailsList = parseSources(caseDetailElastic);

        List<Hit<ElasticSearchCaseDetailsDTO>> hits = new ArrayList<>();
        for (ElasticSearchCaseDetailsDTO caseDetails : caseDetailsList) {
            Hit<ElasticSearchCaseDetailsDTO> hit = mock(Hit.class);
            when(hit.source()).thenReturn(caseDetails);
            when(hit.index()).thenReturn("TestAddressBookCase_cases-000001");
            hits.add(hit);
        }

        HitsMetadata<ElasticSearchCaseDetailsDTO> hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(hits);
        TotalHits totalHits = mock(TotalHits.class);
        when(totalHits.value()).thenReturn(Long.valueOf(caseDetailsList.size()));
        when(hitsMetadata.total()).thenReturn(totalHits);

        MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse = mock(MsearchResponse.class);
        MultiSearchResponseItem multiSearchResponseItem = mock(MultiSearchResponseItem.class);
        when(msearchResponse.responses()).thenReturn(Collections.singletonList(multiSearchResponseItem));
        MultiSearchItem multiSearchItem = mock(MultiSearchItem.class);
        when(multiSearchResponseItem.result()).thenReturn(multiSearchItem);
        when(multiSearchItem.hits()).thenReturn((hitsMetadata));
        when(multiSearchItem.hits().hits()).thenReturn(hits);

        when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
            .thenReturn(msearchResponse);

    }

    public static List<ElasticSearchCaseDetailsDTO> parseSources(String json) throws Exception {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode root = mapper.readTree(json);
        List<ElasticSearchCaseDetailsDTO> caseDetailsList = new ArrayList<>();
        JsonNode hitsArray = root.path("hits").path("hits");
        for (JsonNode hit : hitsArray) {
            JsonNode sourceNode = hit.path("_source");
            ElasticSearchCaseDetailsDTO detailsDTO = mapper.treeToValue(sourceNode, ElasticSearchCaseDetailsDTO.class);
            caseDetailsList.add(detailsDTO);
        }
        return caseDetailsList;
    }
}
