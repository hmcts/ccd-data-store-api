package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
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
    void searchCaseDetailsInvokesOperation() {
        CaseSearchResult result = mock(CaseSearchResult.class);
        CrossCaseTypeSearchRequest preparedRequest = mock(CrossCaseTypeSearchRequest.class);
        when(elasticsearchQueryHelper.prepareRequest(any(), any())).thenReturn(preparedRequest);
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class))).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        List<String> caseTypeIds = singletonList(CASE_TYPE_ID);

        final CaseSearchResult caseSearchResult = endpoint.searchCases(caseTypeIds, searchRequest);

        verify(elasticsearchQueryHelper).prepareRequest(eq(caseTypeIds), eq(searchRequest));
        verify(caseSearchOperation).execute(eq(preparedRequest));
        assertThat(caseSearchResult, is(result));
    }
}
