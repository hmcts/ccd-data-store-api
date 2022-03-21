package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

class CaseSearchEndpointTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_TYPE_ID_2 = "CASE_TYPE_2";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @InjectMocks
    private CaseSearchEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenCaseTypeListIsNull() {
        assertThrows(BadRequestException.class, () ->
            endpoint.searchCases(null, null, true)
        );
    }

    @Test
    void shouldThrowBadRequestExceptionWhenCaseTypeListIsEmpty() {
        List<String> emptyList = List.of();
        assertThrows(BadRequestException.class, () ->
            endpoint.searchCases(emptyList, null, true)
        );
    }

    @Test
    void searchCaseDetailsInvokesOperation() throws JsonProcessingException {

        // ARRANGE
        CaseSearchResult result = mock(CaseSearchResult.class);
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class), anyBoolean())).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        JsonNode searchRequestNode = new ObjectMapper().readTree(searchRequest);
        ElasticsearchRequest elasticSearchRequest = new ElasticsearchRequest(searchRequestNode);
        when(elasticsearchQueryHelper.validateAndConvertRequest(any())).thenReturn(elasticSearchRequest);
        List<String> caseTypeIds = singletonList(CASE_TYPE_ID);

        // ACT
        final CaseSearchResult caseSearchResult = endpoint.searchCases(caseTypeIds, searchRequest, true);

        // ASSERT
        verify(elasticsearchQueryHelper).validateAndConvertRequest(searchRequest);
        verify(caseSearchOperation).execute(argThat(crossCaseTypeSearchRequest -> {
            assertThat(crossCaseTypeSearchRequest.getSearchRequestJsonNode(), is(searchRequestNode));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().size(), is(1));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().get(0), is(CASE_TYPE_ID));
            assertThat(crossCaseTypeSearchRequest.isMultiCaseTypeSearch(), is(false));
            assertThat(crossCaseTypeSearchRequest.getAliasFields().size(), is(0));
            return true;
        }), anyBoolean());
        assertThat(caseSearchResult, is(result));
    }

    @Test
    void searchCaseDetailsInvokesOperationWithExpandedCaseTypesForWildcard() throws JsonProcessingException {

        // ARRANGE
        CaseSearchResult result = mock(CaseSearchResult.class);
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class), anyBoolean())).thenReturn(result);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        JsonNode searchRequestNode = new ObjectMapper().readTree(searchRequest);
        ElasticsearchRequest elasticSearchRequest = new ElasticsearchRequest(searchRequestNode);
        when(elasticsearchQueryHelper.validateAndConvertRequest(any())).thenReturn(elasticSearchRequest);
        List<String> caseTypeIds = singletonList(ElasticsearchRequest.WILDCARD);
        when(elasticsearchQueryHelper.getCaseTypesAvailableToUser()).thenReturn(List.of(CASE_TYPE_ID, CASE_TYPE_ID_2));

        // ACT
        final CaseSearchResult caseSearchResult = endpoint.searchCases(caseTypeIds, searchRequest, true);

        // ASSERT
        verify(elasticsearchQueryHelper).validateAndConvertRequest(searchRequest);
        verify(caseSearchOperation).execute(argThat(crossCaseTypeSearchRequest -> {
            assertThat(crossCaseTypeSearchRequest.getSearchRequestJsonNode(), is(searchRequestNode));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().size(), is(2));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds(), is(List.of(CASE_TYPE_ID, CASE_TYPE_ID_2)));
            assertThat(crossCaseTypeSearchRequest.isMultiCaseTypeSearch(), is(true));
            assertThat(crossCaseTypeSearchRequest.getAliasFields().size(), is(0));
            return true;
        }), anyBoolean());
        assertThat(caseSearchResult, is(result));
    }


    @Nested
    @DisplayName("Build ID lists for LogAudit")
    class BuildIdListsForLogAudit {

        @Test
        void buildCaseIds_shouldReturnEmptyStringWhenEmptyListPassed() {
            assertEquals("", CaseSearchEndpoint.buildCaseIds(createCaseSearchResult(0)));
        }

        @Test
        void buildCaseIds_shouldReturnSimpleStringWhenSingleListItemPassed() {
            assertEquals("1", CaseSearchEndpoint.buildCaseIds(createCaseSearchResult(1)));
        }

        @Test
        void buildCaseIds_shouldReturnCsvStringWhenManyListItemsPassed() {
            assertEquals("1,2,3", CaseSearchEndpoint.buildCaseIds(createCaseSearchResult(3)));
        }

        @Test
        void buildCaseIds_shouldReturnMaxCsvListWhenTooManyListItemsPassed() {

            // ARRANGE
            CaseSearchResult input = createCaseSearchResult(MAX_CASE_IDS_LIST + 1);
            String expectedOutput = createReferenceList(MAX_CASE_IDS_LIST).stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

            // ACT
            String output = CaseSearchEndpoint.buildCaseIds(input);

            // ASSERT
            assertEquals(expectedOutput, output);
        }

        private CaseSearchResult createCaseSearchResult(int numberRequired) {
            List<CaseDetails> caseDetailsList = Lists.newArrayList();

            createReferenceList(numberRequired).forEach(reference -> {
                CaseDetails caseDetails = new CaseDetails();
                caseDetails.setReference(reference);
                caseDetailsList.add(caseDetails);
            });

            return new CaseSearchResult((long) numberRequired, caseDetailsList, null);
        }

        private List<Long> createReferenceList(int numberRequired) {
            List<Long> referenceList = Lists.newArrayList();

            for (int i = 1; i <= numberRequired; i++) {
                referenceList.add((long) i);
            }

            return referenceList;
        }

    }

}
