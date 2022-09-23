package uk.gov.hmcts.ccd.endpoint.std;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchService;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@ExtendWith(MockitoExtension.class)
class GlobalSearchEndpointTest {

    private static final String CASE_TYPE_1 = "case_type_1";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private GlobalSearchService globalSearchService;

    @Mock
    private GlobalSearchParser globalSearchParser;

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
            doReturn(transformedResult).when(globalSearchService).transformResponse(any(), any(), any());
        }

        @DisplayName("should set defaults, assemble query, execute search and transform response")
        @Test
        void shouldSetDefaultsAssembleQueryExecuteSearchAndTransformResponse() {

            // ARRANGE

            // update mock payload with search criteria and a case type
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCcdCaseTypeIds(List.of(CASE_TYPE_1));
            doReturn(searchCriteria).when(globalSearchRequestPayload).getSearchCriteria();

            final List<CaseDetails> filteredCaseList = globalSearchParser.filterCases(searchResults.getCases(),
                                                                        globalSearchRequestPayload.getSearchCriteria());

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
            verify(globalSearchService).transformResponse(globalSearchRequestPayload,
                searchResults.getTotal(), filteredCaseList);

        }

    }


    @Nested
    @DisplayName("Build ID lists for LogAudit")
    class BuildIdListsForLogAudit {

        @Nested
        @DisplayName("build case ID list")
        class BuildCaseIds {

            @DisplayName("List empty: should return empty string when empty list is passed")
            @Test
            void shouldReturnEmptyStringWhenEmptyListPassed() {
                assertEquals(
                    "",
                    GlobalSearchEndpoint.buildCaseIds(createGlobalSearchResponsePayload(0))
                );
            }

            @DisplayName("List one: should return simple string when single list item is passed")
            @Test
            void shouldReturnSimpleStringWhenSingleListItemPassed() {
                assertEquals(
                    "reference-1",
                    GlobalSearchEndpoint.buildCaseIds(createGlobalSearchResponsePayload(1))
                );
            }

            @DisplayName("List many: should return CSV string when many list items are passed")
            @Test
            void shouldReturnCsvStringWhenManyListItemsPassed() {
                assertEquals(
                    "reference-1,reference-2,reference-3",
                    GlobalSearchEndpoint.buildCaseIds(createGlobalSearchResponsePayload(3))
                );
            }

            @DisplayName("List too many: should return max CSV string when too many list items are passed")
            @Test
            void shouldReturnMaxCsvListWhenTooManyListItemsPassed() {

                // ARRANGE
                GlobalSearchResponsePayload input = createGlobalSearchResponsePayload(MAX_CASE_IDS_LIST + 1);
                String expectedOutput = createGlobalSearchResponsePayload(MAX_CASE_IDS_LIST).getResults().stream()
                    .map(GlobalSearchResponsePayload.Result::getCaseReference)
                    .collect(Collectors.joining(","));

                // ACT
                String output = GlobalSearchEndpoint.buildCaseIds(input);

                // ASSERT
                assertEquals(expectedOutput, output);
            }

            private GlobalSearchResponsePayload createGlobalSearchResponsePayload(int numberRequired) {
                List<GlobalSearchResponsePayload.Result> results = Lists.newArrayList();

                for (int i = 1; i <= numberRequired; i++) {
                    results.add(GlobalSearchResponsePayload.Result.builder()
                        .caseReference("reference-" + i)
                        .build()
                    );
                }

                return GlobalSearchResponsePayload.builder()
                    .results(results)
                    .build();
            }
        }


        @Nested
        @DisplayName("build case type ID list")
        class BuildCaseTypeIds {

            @DisplayName("Null check: should return empty string when null request object is passed")
            @Test
            void shouldReturnEmptyStringWhenNullPassed() {
                assertEquals(
                    "",
                    GlobalSearchEndpoint.buildCaseTypeIds(null)
                );
            }

            @DisplayName("Null check: should return empty string when null SearchCriteria object is passed")
            @Test
            void shouldReturnEmptyStringWhenNullSearchCriteriaPassed() {
                assertEquals(
                    "",
                    GlobalSearchEndpoint.buildCaseTypeIds(new GlobalSearchRequestPayload())
                );
            }

            @DisplayName("Null check: should return empty string when null CaseType list is passed")
            @Test
            void shouldReturnEmptyStringWhenNullCaseTypeListPassed() {

                GlobalSearchRequestPayload globalSearchRequestPayload = new GlobalSearchRequestPayload();
                globalSearchRequestPayload.setSearchCriteria(new SearchCriteria());

                assertEquals(
                    "",
                    GlobalSearchEndpoint.buildCaseTypeIds(globalSearchRequestPayload)
                );
            }

            @DisplayName("List empty: should return empty string when empty list is passed")
            @Test
            void shouldReturnEmptyStringWhenEmptyListPassed() {
                assertEquals(
                    "",
                    GlobalSearchEndpoint.buildCaseTypeIds(createGlobalSearchRequestPayload(0))
                );
            }

            @DisplayName("List one: should return simple string when single list item is passed")
            @Test
            void shouldReturnSimpleStringWhenSingleListItemPassed() {
                assertEquals(
                    "case-type-1",
                    GlobalSearchEndpoint.buildCaseTypeIds(createGlobalSearchRequestPayload(1))
                );
            }

            @DisplayName("List many: should return CSV string when many list items are passed")
            @Test
            void shouldReturnCsvStringWhenManyListItemsPassed() {
                assertEquals(
                    "case-type-1,case-type-2,case-type-3",
                    GlobalSearchEndpoint.buildCaseTypeIds(createGlobalSearchRequestPayload(3))
                );
            }

            @DisplayName("List too many: should return max CSV string when too many list items are passed")
            @Test
            void shouldReturnMaxCsvListWhenTooManyListItemsPassed() {

                // ARRANGE
                GlobalSearchRequestPayload input = createGlobalSearchRequestPayload(MAX_CASE_IDS_LIST + 1);
                String expectedOutput = String.join(",", createGlobalSearchRequestPayload(MAX_CASE_IDS_LIST)
                    .getSearchCriteria().getCcdCaseTypeIds());

                // ACT
                String output = GlobalSearchEndpoint.buildCaseTypeIds(input);

                // ASSERT
                assertEquals(expectedOutput, output);
            }

            private GlobalSearchRequestPayload createGlobalSearchRequestPayload(int numberRequired) {
                List<String> caseTypes = Lists.newArrayList();

                for (int i = 1; i <= numberRequired; i++) {
                    caseTypes.add("case-type-" + i);
                }

                SearchCriteria searchCriteria = new SearchCriteria();
                searchCriteria.setCcdCaseTypeIds(caseTypes);

                GlobalSearchRequestPayload globalSearchRequestPayload = new GlobalSearchRequestPayload();
                globalSearchRequestPayload.setSearchCriteria(searchCriteria);

                return globalSearchRequestPayload;
            }

        }

    }

}
