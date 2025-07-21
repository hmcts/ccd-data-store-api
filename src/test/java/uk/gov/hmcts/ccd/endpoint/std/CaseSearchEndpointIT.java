package uk.gov.hmcts.ccd.endpoint.std;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SuppressWarnings("checkstyle:OperatorWrap")
class CaseSearchEndpointIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    @MockBean
    private ElasticsearchClient elasticsearchClient;

    @SpyBean
    private AuditRepository auditRepository;

    private static final String POST_SEARCH_CASES = "/searchCases";
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void testSearchCaseDetails() throws Exception {
        stubElasticSearchSearchRequestWillReturn("1535450291607660");

        String searchRequest = "{\"query\": {\"match_all\": {}}}";

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(JSON_CONTENT_TYPE)
                .param("ctid", "TestAddressBookCase")
                .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString, CaseSearchResult.class);

        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(1));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607660L))));
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
        stubElasticSearchSearchRequestWillReturn("1535450291607660", "1535450291607670");

        String searchRequest = "{\"query\": {\"match_all\": {}}}";

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(JSON_CONTENT_TYPE)
                .param("ctid", "TestAddressBookCase", "TestAddressBookCase4")
                .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString, CaseSearchResult.class);

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

    private void stubElasticSearchSearchRequestWillReturn(String... references) throws IOException {
        List<Hit<CaseDetails>> hits = Arrays.asList(references).stream()
            .map(ref -> {
                CaseDetails caseDetails = new CaseDetails();
                caseDetails.setReference(Long.parseLong(ref));
                caseDetails.setJurisdiction("PROBATE");
                caseDetails.setCaseTypeId("TestAddressBookCase");
                caseDetails.setState("TODO");
                caseDetails.setCreatedDate(LocalDateTime.parse("2018-08-28T09:58:11.627"));
                caseDetails.setLastModified(LocalDateTime.parse("2018-08-28T09:58:11.643"));
                caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

                // Build Hit<T> properly
                return new Hit.Builder<CaseDetails>().source(caseDetails).index("case_ca_001").build();
            })
            .collect(Collectors.toList());

        HitsMetadata<CaseDetails> hitsMetadata = new HitsMetadata.Builder<CaseDetails>()
            .hits(hits)
            .total(t -> t
                .value(hits.size())
                .relation(TotalHitsRelation.Eq)
            )
            .build();

        SearchResponse<CaseDetails> mockResponse = new SearchResponse.Builder<CaseDetails>()
            .hits(hitsMetadata)
            .took(100)
            .timedOut(false)
            .shards(s -> s
                .total(1)
                .successful(1)
                .skipped(0)
                .failed(0)
            )
            .build();

        when(elasticsearchClient.search(any(SearchRequest.class), eq(CaseDetails.class))).thenReturn(mockResponse);
    }
}
