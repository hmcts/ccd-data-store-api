package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private static final String CASE_TYPE_ID = "casetypeid";
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
        searchRequestJsonNode.set(QUERY_NAME, mock(ObjectNode.class));
    }

    @Test
    void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.isSucceeded()).thenReturn(true);
        when(searchResult.getTotal()).thenReturn(1L);
        when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
        when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class)).thenReturn(caseDetailsDTO);
        when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
        when(jestClient.execute(any(Search.class))).thenReturn(searchResult);
        CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID, searchRequestJsonNode);
        when(caseSearchRequestSecurity.createSecuredSearchRequest(request)).thenReturn(request);

        CaseSearchResult caseSearchResult = searchOperation.execute(request);

        assertThat(caseSearchResult.getCases(), equalTo(newArrayList(caseDetails)));
        assertThat(caseSearchResult.getTotal(), equalTo(1L));
        ArgumentCaptor<Search> arg = ArgumentCaptor.forClass(Search.class);
        verify(jestClient).execute(arg.capture());
        Search searchRequest = arg.getValue();
        assertThat(searchRequest.getIndex(), equalTo(toIndex(CASE_TYPE_ID)));
        assertThat(searchRequest.getType(), equalTo(INDEX_TYPE));
        verify(caseSearchRequestSecurity).createSecuredSearchRequest(request);
    }

    @Test
    void searchShouldReturnBadSearchRequestOnFailure() throws IOException {
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.isSucceeded()).thenReturn(false);
        when(jestClient.execute(any(Search.class))).thenReturn(searchResult);
        CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID, searchRequestJsonNode);
        when(caseSearchRequestSecurity.createSecuredSearchRequest(request)).thenReturn(request);

        assertThrows(BadSearchRequest.class, () -> searchOperation.execute(request));
    }

    private String toIndex(String caseTypeId) {
        return String.format(INDEX_NAME_FORMAT, caseTypeId);
    }
}
