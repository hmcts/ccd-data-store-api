package uk.gov.hmcts.ccd.endpoint.std;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseDetailsSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CaseDetailsSearchEndpointTest {

    private static final String CASE_TYPE_ID = "GrantOnly";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDetailsSearchOperation caseDetailsSearchOperation;

    @InjectMocks
    private CaseDetailsSearchEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

        @Test
        void searchCaseDetailsThrowsExceptionWhenNoQueryProvided() {
        String searchRequest = "{\n"
                + "\"from\" : 0,\n"
                + "\"size\" : 3\n"
                + "}";
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("query_string"));

        assertThrows(BadSearchRequest.class,
                     () -> endpoint.searchCases(CASE_TYPE_ID, searchRequest));

            verify(caseDetailsSearchOperation, never()).execute(CASE_TYPE_ID, searchRequest);
    }

    @Test
    void searchCaseDetailsRejectsBlacklistedSearchQueries() {
        String searchRequest = "{  \n"
            + "   \"query\":{  \n"
            + "      \"bool\":{  \n"
            + "         \"must\":[  \n"
            + "            {  \n"
            + "               \"simple_query_string\":{  \n"
            + "                  \"query\":\"isde~2\"\n"
            + "               }\n"
            + "            },\n"
            + "            {  \n"
            + "               \"query_string\":{  \n"
            + "                  \"query\":\"isde~2\"\n"
            + "               }\n"
            + "            },\n"
            + "            {  \n"
            + "               \"range\":{  \n"
            + "                  \"data.ComplexField.ComplexNestedField.NestedNumberField\":{  \n"
            + "                     \"lt\":\"91\"\n"
            + "                  }\n"
            + "               }\n"
            + "            }\n"
            + "         ]\n"
            + "      }\n"
            + "   }\n"
            + "}";
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("query_string"));

        assertThrows(BadSearchRequest.class,
                     () -> endpoint.searchCases(CASE_TYPE_ID, searchRequest));

        verify(caseDetailsSearchOperation, never()).execute(CASE_TYPE_ID, searchRequest);
    }

    @Test
    void searchCaseDetailsAllowsQueriesNotBlacklisted() throws IOException {
        String query = "{\n"
            + "   \"query\":{\n"
            + "      \"bool\":{\n"
            + "         \"must\":[\n"
            + "            {\n"
            + "               \"simple_query_string\":{\n"
            + "                  \"query\":\"isde~2\"\n"
            + "               }\n"
            + "            },\n"
            + "            {\n"
            + "               \"range\":{\n"
            + "                  \"data.ComplexField.ComplexNestedField.NestedNumberField\":{\n"
            + "                     \"lt\":\"91\"\n"
            + "                  }\n"
            + "               }\n"
            + "            }\n"
            + "         ]\n"
            + "      }\n"
            + "   }\n"
            + "}";
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("query_string"));

        endpoint.searchCases(CASE_TYPE_ID, query);

        verify(caseDetailsSearchOperation).execute(CASE_TYPE_ID, query);
    }

    @Test
    void searchCaseDetailsInvokesOperation() throws IOException {
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("blockedQuery"));
        CaseDetailsSearchResult result = mock(CaseDetailsSearchResult.class);
        Mockito.when(caseDetailsSearchOperation.execute(anyString(), anyString())).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";

        CaseDetailsSearchResult caseDetailsSearchResult = endpoint.searchCases(CASE_TYPE_ID, searchRequest);

        verify(caseDetailsSearchOperation).execute(CASE_TYPE_ID, searchRequest);
        assertThat(caseDetailsSearchResult, is(result));
    }
}
