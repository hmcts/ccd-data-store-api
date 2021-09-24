package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalSearchEndpointTest {

    private static final String CASE_TYPE_1 = "case_type_1";
    private static final String CASE_TYPE_2 = "case_type_2";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Mock
    private GlobalSearchService globalSearchService;

    @InjectMocks
    private GlobalSearchEndpoint classUnderTest;

    @Nested
    @DisplayName("searchForCases")
    class SearchForCases {

        private GlobalSearchRequestPayload globalSearchRequestPayload;
        private CrossCaseTypeSearchRequest assembledSearchRequest;
        private CaseSearchResult searchResults;
        private GlobalSearchResponsePayload transformedResult;

        @BeforeEach
        void setUp() {
            globalSearchRequestPayload = Mockito.spy(GlobalSearchRequestPayload.class);
            assembledSearchRequest = Mockito.mock(CrossCaseTypeSearchRequest.class);
            searchResults = Mockito.mock(CaseSearchResult.class);
            transformedResult = GlobalSearchResponsePayload.builder().build();

            doReturn(assembledSearchRequest).when(globalSearchService).assembleSearchQuery(any());
            doReturn(searchResults).when(caseSearchOperation).execute(any(), anyBoolean());
            doReturn(transformedResult).when(globalSearchService).transformResponse(any(), any());
        }

        @DisplayName("should set defaults, assemble query, execute search and transform response")
        @Test
        void shouldSetDefaultsAssembleQueryExecuteSearchAndTransformResponse() {

            // ARRANGE

            // update mock payload with search criteria and a case type
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCcdCaseTypeIds(List.of(CASE_TYPE_1));
            doReturn(searchCriteria).when(globalSearchRequestPayload).getSearchCriteria();

            // ACT
            GlobalSearchResponsePayload output = classUnderTest.searchForCases(globalSearchRequestPayload);

            // ASSERT
            assertEquals(transformedResult, output);
            // verify internal calls
            // :: set defaults
            verify(globalSearchRequestPayload).setDefaults();
            // :: assemble query
            verify(globalSearchService).assembleSearchQuery(globalSearchRequestPayload);
            // :: execute search
            verify(caseSearchOperation).execute(eq(assembledSearchRequest), anyBoolean());
            // :: TransformResponse
            verify(globalSearchService).transformResponse(globalSearchRequestPayload, searchResults);

        }

        @ParameterizedTest(name = "should populate case types in search criteria if null or empty: caseTypeIds = {0}")
        @NullAndEmptySource
        void shouldPopulateCaseTypesInSearchCriteriaIfNull(List<String> caseTypeIds) {

            // ARRANGE
            doReturn(List.of(CASE_TYPE_1, CASE_TYPE_2)).when(elasticsearchQueryHelper).getCaseTypesAvailableToUser();

            // update mock payload with search criteria and supplied case type list
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCcdCaseTypeIds(caseTypeIds);
            doReturn(searchCriteria).when(globalSearchRequestPayload).getSearchCriteria();

            // ACT
            GlobalSearchResponsePayload output = classUnderTest.searchForCases(globalSearchRequestPayload);

            // ASSERT
            assertEquals(transformedResult, output);
            // verify different internal calls
            // :: load case types
            verify(elasticsearchQueryHelper).getCaseTypesAvailableToUser();
            // :: assemble query using loaded case types
            ArgumentCaptor<GlobalSearchRequestPayload> captor
                = ArgumentCaptor.forClass(GlobalSearchRequestPayload.class);
            verify(globalSearchService).assembleSearchQuery(captor.capture());
            assertTrue(captor.getValue().getSearchCriteria().getCcdCaseTypeIds().containsAll(
                List.of(CASE_TYPE_1, CASE_TYPE_2)
            ));

        }

    }

}
