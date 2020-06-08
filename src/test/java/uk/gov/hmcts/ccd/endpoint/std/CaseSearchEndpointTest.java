package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CaseSearchEndpointTest {

    private static final String CASE_TYPE_ID = "GrantOnly";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @InjectMocks
    private CaseSearchEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void searchCaseDetailsInvokesOperation() throws JsonProcessingException {
        CaseSearchResult result = mock(CaseSearchResult.class);
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class))).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        JsonNode searchRequestNode = new ObjectMapper().readTree(searchRequest);
        ElasticsearchRequest elasticSearchRequest = new ElasticsearchRequest(searchRequestNode);
        when(elasticsearchQueryHelper.validateAndConvertRequest(any())).thenReturn(elasticSearchRequest);
        List<String> caseTypeIds = singletonList(CASE_TYPE_ID);

        final CaseSearchResult caseSearchResult = endpoint.searchCases(caseTypeIds, searchRequest);

        verify(elasticsearchQueryHelper).validateAndConvertRequest(eq(searchRequest));
        verify(caseSearchOperation).execute(argThat(crossCaseTypeSearchRequest -> {
            assertThat(crossCaseTypeSearchRequest.getSearchRequestJsonNode(), is(searchRequestNode));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().size(), is(1));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().get(0), is(CASE_TYPE_ID));
            assertThat(crossCaseTypeSearchRequest.isMultiCaseTypeSearch(), is(false));
            assertThat(crossCaseTypeSearchRequest.getAliasFields().size(), is(0));
            return true;
        }));
        assertThat(caseSearchResult, is(result));
    }
}
