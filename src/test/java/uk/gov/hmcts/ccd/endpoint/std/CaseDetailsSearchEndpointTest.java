package uk.gov.hmcts.ccd.endpoint.std;

import java.io.IOException;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseDetailsSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CaseDetailsSearchEndpointTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String QUERY = "{\"query\":{}}";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDetailsSearchOperation caseDetailsSearchOperation;

    @Mock
    private ObjectMapperService objectMapperService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CaseDetailsSearchEndpoint endpoint;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        when(objectMapperService.convertStringToObject(anyString(), any())).thenReturn(objectMapper.readValue(QUERY, ObjectNode.class));
    }

    @Test
    void searchCaseDetailsThrowsExceptionWhenNoQueryProvided() throws IOException {
        String searchRequest = "{\n"
            + "\"from\" : 0,\n"
            + "\"size\" : 3\n"
            + "}";
        when(applicationParams.getSearchBlackList()).thenReturn(Collections.singletonList("query_string"));
        when(objectMapperService.convertStringToObject(searchRequest, ObjectNode.class)).thenReturn(objectMapper.readValue(searchRequest, ObjectNode.class));

        assertThrows(BadSearchRequest.class, () -> endpoint.searchCases(CASE_TYPE_ID, searchRequest));
        verifyZeroInteractions(caseDetailsSearchOperation);
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

        assertThrows(BadSearchRequest.class, () -> endpoint.searchCases(CASE_TYPE_ID, searchRequest));
        verifyZeroInteractions(caseDetailsSearchOperation);
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

        verify(caseDetailsSearchOperation).execute(any(CaseSearchRequest.class));
    }

    @Test
    void searchCaseDetailsInvokesOperation() throws IOException {
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("blockedQuery"));
        CaseDetailsSearchResult result = mock(CaseDetailsSearchResult.class);
        when(caseDetailsSearchOperation.execute(any(CaseSearchRequest.class))).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";

        CaseDetailsSearchResult caseDetailsSearchResult = endpoint.searchCases(CASE_TYPE_ID, searchRequest);

        verify(caseDetailsSearchOperation).execute(any(CaseSearchRequest.class));
        assertThat(caseDetailsSearchResult, is(result));
    }
}
