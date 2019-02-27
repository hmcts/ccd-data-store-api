package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY;
import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchCaseSearchOperation.MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class ElasticsearchCaseSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final String CASE_TYPE_ID_1 = "casetypeid1";
    private static final String CASE_TYPE_ID_2 = "casetypeid2";
    private static final String INDEX_TYPE = "case";
    private final String caseDetailsElastic = "{some case details}";

    @InjectMocks
    private ElasticsearchCaseSearchOperation searchOperation;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private JestClient jestClient;

    @Mock
    private CaseDetailsMapper mapper;

    @Mock
    private ElasticSearchCaseDetailsDTO caseDetailsDTO;

    @Mock
    private CaseSearchRequestSecurity caseSearchRequestSecurity;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private ObjectMapper objectMapper;

    private final ObjectNode searchRequestJsonNode = JsonNodeFactory.instance.objectNode();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
        searchRequestJsonNode.set(QUERY, mock(ObjectNode.class));
    }

    @Nested
    @DisplayName("Single case type search")
    class SingleCaseTypeSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for a single case type and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            SearchResult searchResult = mock(SearchResult.class);
            when(searchResult.getTotal()).thenReturn(1L);
            when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class)).thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, searchRequestJsonNode);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(searchRequestJsonNode)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class))).thenReturn(request);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest);

            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(newArrayList(caseDetails))),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(1L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(applicationParams).getCasesIndexNameFormat(),
                () -> verify(applicationParams).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity).createSecuredSearchRequest(any(CaseSearchRequest.class)));
        }

    }

    @Nested
    @DisplayName("Multi case type search")
    class MultiCaseTypeSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for multiple case types and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);

            SearchResult searchResult1 = mock(SearchResult.class);
            when(searchResult1.getTotal()).thenReturn(10L);
            when(searchResult1.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response1 = mock(MultiSearchResult.MultiSearchResponse.class);
            setInternalState(response1, "searchResult", searchResult1);

            SearchResult searchResult2 = mock(SearchResult.class);
            when(searchResult2.getTotal()).thenReturn(10L);
            when(searchResult2.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response2 = mock(MultiSearchResult.MultiSearchResponse.class);
            setInternalState(response2, "searchResult", searchResult2);
            when(multiSearchResult.getResponses()).thenReturn(asList(response1, response2));

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class)).thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request1 = new CaseSearchRequest(CASE_TYPE_ID_1, searchRequestJsonNode);
            CaseSearchRequest request2 = new CaseSearchRequest(CASE_TYPE_ID_2, searchRequestJsonNode);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(searchRequestJsonNode)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class))).thenReturn(request1, request2);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest);

            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(newArrayList(caseDetails, caseDetails))),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(20L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(applicationParams, times(2)).getCasesIndexNameFormat(),
                () -> verify(applicationParams, times(2)).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity, times(2)).createSecuredSearchRequest(any(CaseSearchRequest.class)));
        }

    }

    @Nested
    @DisplayName("Elasticsearch search failure")
    class SearchFailure {

        @Test
        @DisplayName("should throw exception when Elasticsearch search does not succeed")
        void searchShouldReturnBadSearchRequestOnFailure() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(false);
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);
            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, searchRequestJsonNode);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(searchRequestJsonNode)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class))).thenReturn(request);

            assertThrows(BadSearchRequest.class, () -> searchOperation.execute(crossCaseTypeSearchRequest));
        }

        @Test
        @DisplayName("should throw exception when Elasticsearch multi-search response returns error")
        void searchShouldReturnBadSearchRequestOnResponseError() throws IOException {
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            JsonElement error = mock(JsonElement.class);
            setInternalState(response, "isError", true);
            setInternalState(response, "error", error);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty(MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE, "error msg");
            when(response.error.getAsJsonObject()).thenReturn(errorObject);
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);
            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, searchRequestJsonNode);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(searchRequestJsonNode)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class))).thenReturn(request);

            assertThrows(BadSearchRequest.class, () -> searchOperation.execute(crossCaseTypeSearchRequest));
        }

    }

}
