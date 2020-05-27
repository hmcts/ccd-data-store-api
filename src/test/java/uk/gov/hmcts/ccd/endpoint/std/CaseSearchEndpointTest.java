package uk.gov.hmcts.ccd.endpoint.std;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CaseSearchEndpointTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String QUERY = "{\"query\":{}}";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ObjectMapperService objectMapperService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CaseSearchEndpoint endpoint;

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
        when(applicationParams.getSearchBlackList()).thenReturn(singletonList("query_string"));
        when(objectMapperService.convertStringToObject(searchRequest, JsonNode.class)).thenReturn(objectMapper.readValue(searchRequest, ObjectNode.class));

        assertThrows(BadSearchRequest.class, () -> endpoint.searchCases(singletonList(CASE_TYPE_ID), searchRequest));
        verifyZeroInteractions(caseSearchOperation);
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

        assertThrows(BadSearchRequest.class, () -> endpoint.searchCases(singletonList(CASE_TYPE_ID), searchRequest));
        verifyZeroInteractions(caseSearchOperation);
    }

    @Test
    void searchCaseDetailsAllowsQueriesNotBlacklisted() {
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

        endpoint.searchCases(singletonList(CASE_TYPE_ID), query);

        verify(caseSearchOperation).executeExternal(any(CrossCaseTypeSearchRequest.class));
    }

    @Test
    void searchCaseDetailsInvokesOperation() {
        given(applicationParams.getSearchBlackList()).willReturn(newArrayList("blockedQuery"));
        CaseSearchResult result = mock(CaseSearchResult.class);
        when(caseSearchOperation.executeExternal(any(CrossCaseTypeSearchRequest.class))).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";

        CaseSearchResult caseSearchResult = endpoint.searchCases(singletonList(CASE_TYPE_ID), searchRequest);

        verify(caseSearchOperation).executeExternal(any(CrossCaseTypeSearchRequest.class));
        assertThat(caseSearchResult, is(result));
    }
}
