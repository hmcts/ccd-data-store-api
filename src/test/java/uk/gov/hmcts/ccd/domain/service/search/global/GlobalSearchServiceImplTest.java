package uk.gov.hmcts.ccd.domain.service.search.global;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceImplTest extends TestFixtures {

    private static final String GLOBAL_SEARCH_INDEX_NAME = "GS_Index";
    private static final String GLOBAL_SEARCH_INDEX_TYPE = "GS_Type";

    @Mock
    private ApplicationParams applicationParams;

    @Spy
    @SuppressWarnings("unused") // needed to set up GlobalSearchServiceImpl
    private ObjectMapperService objectMapperService = new DefaultObjectMapperService(new ObjectMapper());

    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @Mock
    private GlobalSearchResponseTransformer globalSearchResponseTransformer;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Mock
    private GlobalSearchQueryBuilder globalSearchQueryBuilder;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private GlobalSearchParser globalSearchParser;

    @InjectMocks
    private GlobalSearchServiceImpl underTest;

    @Test
    void testLocationLookupBuilderFunction() {
        // WHEN
        final LocationLookup locationLookup =
            GlobalSearchServiceImpl.LOCATION_LOOKUP_FUNCTION.apply(locationsRefData);

        // THEN
        assertThat(locationLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getLocationName("321")).isEqualTo("Location 1");
                assertThat(dictionary.getLocationName("L-0")).isNull();
                assertThat(dictionary.getLocationName(null)).isNull();
                assertThat(dictionary.getRegionName("123")).isEqualTo("Region 2");
                assertThat(dictionary.getRegionName("R-0")).isNull();
                assertThat(dictionary.getRegionName(null)).isNull();
            });
    }

    @Test
    void testServiceLookupBuilderFunction() {
        // WHEN
        final ServiceLookup serviceLookup = GlobalSearchServiceImpl.SERVICE_LOOKUP_FUNCTION.apply(servicesRefData);

        // THEN
        assertThat(serviceLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getServiceShortDescription("SC1")).isEqualTo("Service 1");
                assertThat(dictionary.getServiceShortDescription("SC2")).isEqualTo("Service 2");
                assertThat(dictionary.getServiceShortDescription("SC3")).isNull();
                assertThat(dictionary.getServiceShortDescription(null)).isNull();
            });
    }

    @Test
    void testShouldTransformSearchResult() {
        // GIVEN
        final GlobalSearchRequestPayload requestPayload = new GlobalSearchRequestPayload();
        final CaseSearchResult caseSearchResult = buildCaseSearchResult();
        List<CaseDetails> filteredCaseList = caseSearchResult.getCases();
        doReturn(servicesRefData).when(referenceDataRepository).getServices();
        doReturn(locationsRefData).when(referenceDataRepository).getBuildingLocations();
        doReturn(GlobalSearchResponsePayload.ResultInfo.builder().build())
            .when(globalSearchResponseTransformer).transformResultInfo(
                requestPayload.getMaxReturnRecordCount(),
                requestPayload.getStartRecordNumber(),
                caseSearchResult.getTotal(),
                caseSearchResult.getCases().size()
            );
        doReturn(GlobalSearchResponsePayload.Result.builder().build())
            .when(globalSearchResponseTransformer).transformResult(
                any(CaseDetails.class),
                any(ServiceLookup.class),
                any(LocationLookup.class)
            );

        Mockito.lenient()
            .when(globalSearchParser.filterCases(caseSearchResult.getCases(), requestPayload.getSearchCriteria()))
            .thenReturn(filteredCaseList);

        // WHEN
        final GlobalSearchResponsePayload response = underTest.transformResponse(requestPayload,
            caseSearchResult.getTotal(), filteredCaseList);

        // THEN
        assertThat(response).isNotNull();

        verify(globalSearchResponseTransformer).transformResultInfo(
            requestPayload.getMaxReturnRecordCount(),
            requestPayload.getStartRecordNumber(),
            caseSearchResult.getTotal(),
            caseSearchResult.getCases().size()
        );
        verify(globalSearchResponseTransformer).transformResult(
            any(CaseDetails.class),
            any(ServiceLookup.class),
            any(LocationLookup.class)
        );
    }

    private CaseSearchResult buildCaseSearchResult() {
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withReference(1629297445116784L)
            .withState("CaseCreated")
            .withJurisdiction(JURISDICTION_ID)
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();
        return new CaseSearchResult(1L, List.of(caseDetails));
    }


    @Nested
    @DisplayName("AssembleSearchQuery")
    class AssembleSearchQuery {

        GlobalSearchRequestPayload request;
        QueryBuilder expectedBuilder;
        ElasticsearchRequest expectedElasticsearchRequest;

        private final ObjectMapper mapper = new ObjectMapper();

        @DisplayName("Null Check: should return null if supplied with null request or query")
        @Test
        void shouldReturnNullForNullRequestOrQuery() {

            assertNull(underTest.assembleSearchQuery(null));
            assertNull(underTest.assembleSearchQuery(new GlobalSearchRequestPayload()));

        }

        @DisplayName("Query Check: should build query from request and convert result to an ElasticSearchRequest")
        @Test
        void shouldBuildQueryAndConvertToElasticSearchRequest() throws Exception {

            // ARRANGE
            mockInternalCalls();

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify query build
            verify(globalSearchQueryBuilder).globalSearchQuery(request);

            // ::  verify build response passed to convert
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            JSONAssert.assertEquals(
                "{\"query\":" + expectedBuilder + "}", // expects simple query wrapper
                jsonSearchRequestCaptor.getValue(),
                JSONCompareMode.LENIENT
            );

            // ::  verify converted value in output
            assertEquals(expectedElasticsearchRequest, output.getElasticSearchRequest());
        }

        @DisplayName("Query Check: should copy raw case types list to output")
        @Test
        void shouldCopyRequestCaseTypeListToOutput() throws Exception {

            // ARRANGE
            mockInternalCalls();

            // add list of case types to request
            request.getSearchCriteria().setCcdCaseTypeIds(List.of("case_type_1", "case_type_2"));

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertAll(
                () -> assertEquals(
                    request.getSearchCriteria().getCcdCaseTypeIds().size(),
                    output.getCaseTypeIds().size()
                ),
                () -> assertTrue(
                    request.getSearchCriteria().getCcdCaseTypeIds().containsAll(output.getCaseTypeIds())
                ),
                // NB: verify jurisdiction lookup not used when case types supplied
                () -> verify(caseDefinitionRepository, never()).getCaseTypesIDsByJurisdictions(anyList())
            );
        }

        @ParameterizedTest(
            name = "Query Check: should populate case types from jurisdictions lookup if null or empty: {0}"
        )
        @NullAndEmptySource
        void shouldPopulateCaseTypesFromJurisdictionsLookupIfNull(List<String> caseTypeIds) throws Exception {

            // ARRANGE
            mockInternalCalls();

            // setup case type and jurisdiction relationships
            List<String> jurisdictions = List.of("jurisdictions_1", "jurisdictions_2");
            List<String> expectedCaseTypes = List.of("case_type_1", "case_type_2");
            doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesIDsByJurisdictions(jurisdictions);

            // add list of jurisdictions to request
            request.getSearchCriteria().setCcdCaseTypeIds(caseTypeIds); // i.e. @NullAndEmptySource
            request.getSearchCriteria().setCcdJurisdictionIds(jurisdictions);

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertAll(
                () -> assertEquals(expectedCaseTypes.size(), output.getCaseTypeIds().size()),
                () -> assertTrue(expectedCaseTypes.containsAll(output.getCaseTypeIds())),

                // NB: verify jurisdiction lookup used when case types are missing
                () -> verify(caseDefinitionRepository, times(1))
                    .getCaseTypesIDsByJurisdictions(jurisdictions)
            );
        }

        @DisplayName("Fields Check: should generate search request with source fields")
        @Test
        void shouldGenerateSearchRequestWithSourceFields() throws Exception {

            // ARRANGE
            mockInternalCalls();

            String sourceField1 = GlobalSearchFields.CaseDataPaths.OTHER_REFERENCE;
            String sourceField2 = GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY;
            doReturn(JacksonUtils.MAPPER.createArrayNode()
                .add(sourceField1)
                .add(sourceField2)).when(globalSearchQueryBuilder).globalSearchSourceFields();

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify source fields loaded
            verify(globalSearchQueryBuilder).globalSearchSourceFields();

            // ::  verify build response passed to convert: includes source list
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            JSONAssert.assertEquals(
                "{\"query\":" + expectedBuilder + ","
                    + "\"_source\": [\"" + sourceField1 + "\",\"" + sourceField2 + "\"]}",
                jsonSearchRequestCaptor.getValue(),
                JSONCompareMode.LENIENT
            );
        }

        @DisplayName("Fields Check: should generate search request with SupplementaryData fields")
        @Test
        void shouldGenerateSearchRequestWithSupplementaryDataFields() throws Exception {

            // ARRANGE
            mockInternalCalls();

            String supplementaryDataField1 = GlobalSearchFields.SupplementaryDataFields.SERVICE_ID;
            doReturn(JacksonUtils.MAPPER.createArrayNode()
                .add(supplementaryDataField1)).when(globalSearchQueryBuilder).globalSearchSupplementaryDataFields();

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify SupplementaryData fields loaded
            verify(globalSearchQueryBuilder).globalSearchSupplementaryDataFields();

            // ::  verify output includes SupplementaryData list
            assertNotNull(output.getElasticSearchRequest());
            assertNotNull(output.getElasticSearchRequest().getRequestedSupplementaryData());
            ArrayNode supplementaryData = output.getElasticSearchRequest().getRequestedSupplementaryData();
            assertEquals(supplementaryDataField1, supplementaryData.get(0).asText());
            assertAll(
                () -> assertEquals(1, supplementaryData.size()),
                () -> assertEquals(supplementaryDataField1, supplementaryData.get(0).asText())
            );
        }

        @DisplayName("Index Check: should generate search request for global search index")
        @Test
        void shouldGenerateSearchRequestForGlobalSearchIndex() throws Exception {

            // ARRANGE
            mockInternalCalls();

            doReturn(GLOBAL_SEARCH_INDEX_NAME).when(applicationParams).getGlobalSearchIndexName();
            doReturn(GLOBAL_SEARCH_INDEX_TYPE).when(applicationParams).getGlobalSearchIndexType();

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertTrue(output.getSearchIndex().isPresent());
            assertAll(
                () -> assertEquals(
                    GLOBAL_SEARCH_INDEX_NAME,
                    output.getSearchIndex().orElseThrow().getIndexName()
                ),
                () -> assertEquals(
                    GLOBAL_SEARCH_INDEX_TYPE,
                    output.getSearchIndex().orElseThrow().getIndexType()
                )
            );
        }

        @DisplayName("Sort Check: should build sort from request and convert result to an ElasticSearchRequest")
        @Test
        void shouldBuildSortAndConvertToElasticSearchRequest() throws Exception {

            // ARRANGE
            mockInternalCalls();

            var sort1 = createSimpleSortBuilder("sort_1");
            var sort2 = createSimpleSortBuilder("sort_2");
            doReturn(List.of(sort1, sort2)).when(globalSearchQueryBuilder).globalSearchSort(any());

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: including sort
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            JSONAssert.assertEquals(
                "{\"query\":" + expectedBuilder + "," + "\"sort\":[" + sort1 + "," + sort2 + "]}",
                jsonSearchRequestCaptor.getValue(),
                JSONCompareMode.STRICT_ORDER // NB: sort order must be preserved
            );

        }

        @DisplayName("Sort Check: should skip sort if not in global search builder response")
        @Test
        void shouldSkipSortIfNotInBuilderResponse() throws Exception {

            // ARRANGE
            mockInternalCalls();

            doReturn(List.of()).when(globalSearchQueryBuilder).globalSearchSort(any()); // i.e. return empty sort

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: excludes sort
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            assertFalse(jsonSearchRequestCaptor.getValue().contains("sort"));

        }

        @DisplayName("Pagination Check: Max Records: should include max records limit in query conversion")
        @Test
        void shouldBuildPaginationQueryWithMaxLimitAndConvertToElasticSearchRequest() throws Exception {

            // ARRANGE
            mockInternalCalls();

            int maxReturnRecordCount = 100;
            request.setMaxReturnRecordCount(maxReturnRecordCount);

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: includes max record limit
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            JSONAssert.assertEquals(
                "{\"query\":" + expectedBuilder + "," + "\"size\":" + maxReturnRecordCount + "}",
                jsonSearchRequestCaptor.getValue(),
                JSONCompareMode.LENIENT
            );

        }

        @DisplayName("Pagination Check: Max Records: should exclude max records limit if zero")
        @Test
        void shouldSkipMaxRecordLimitIfZero() throws Exception {

            // ARRANGE
            mockInternalCalls();

            int maxReturnRecordCount = 0;
            request.setMaxReturnRecordCount(maxReturnRecordCount);
            int startRecordNumber = 99;
            request.setStartRecordNumber(startRecordNumber);

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: excludes max record limit
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            assertFalse(jsonSearchRequestCaptor.getValue().contains("size"));

        }

        @DisplayName("Pagination Check: Start Record: should include start record in query conversion")
        @Test
        void shouldBuildPaginationQueryWithStartRecordAndConvertToElasticSearchRequest() throws Exception {

            // ARRANGE
            mockInternalCalls();

            int startRecordNumber = 100;
            request.setStartRecordNumber(startRecordNumber);

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: includes max record limit
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            // NB: `GS.StartRecordNumber` is not zero indexed but `ES.from` is: hence = {start - 1}
            JSONAssert.assertEquals(
                "{\"query\":" + expectedBuilder + "," + "\"from\":" + (startRecordNumber - 1) + "}",
                jsonSearchRequestCaptor.getValue(),
                JSONCompareMode.LENIENT
            );

        }

        @DisplayName("Pagination Check: Start Records: should exclude start record if zero")
        @Test
        void shouldSkipStartRecordIfZero() throws Exception {

            // ARRANGE
            mockInternalCalls();

            int maxReturnRecordCount = 100;
            request.setMaxReturnRecordCount(maxReturnRecordCount);
            int startRecordNumber = 0;
            request.setStartRecordNumber(startRecordNumber);

            // ACT
            CrossCaseTypeSearchRequest output = underTest.assembleSearchQuery(request);

            // ASSERT
            assertNotNull(output);

            // ::  verify sort build
            verify(globalSearchQueryBuilder).globalSearchSort(request);

            // ::  verify build response passed to convert: excludes max record limit
            ArgumentCaptor<String> jsonSearchRequestCaptor = verifyValidateAndConvertRequest_andGetCaptor();
            assertFalse(jsonSearchRequestCaptor.getValue().contains("from"));

        }

        private QueryBuilder createSimpleQueryBuilder() {
            return QueryBuilders.matchAllQuery();
        }

        private FieldSortBuilder createSimpleSortBuilder(String fieldName) {
            return SortBuilders
                .fieldSort(fieldName);
        }

        private ElasticsearchRequest createSimpleElasticsearchRequest() throws JsonProcessingException {
            JsonNode queryNode = mapper.readTree("{\"query\":" + createSimpleQueryBuilder() + "}");
            return new ElasticsearchRequest(queryNode);
        }

        private void mockInternalCalls() throws JsonProcessingException {

            request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(new SearchCriteria()); // NB: must not be empty

            expectedBuilder = createSimpleQueryBuilder();
            doReturn(expectedBuilder).when(globalSearchQueryBuilder).globalSearchQuery(any());

            expectedElasticsearchRequest = createSimpleElasticsearchRequest();
            doReturn(expectedElasticsearchRequest).when(elasticsearchQueryHelper).validateAndConvertRequest(any());
        }

        private ArgumentCaptor<String> verifyValidateAndConvertRequest_andGetCaptor() {
            ArgumentCaptor<String> jsonSearchRequestCaptor = ArgumentCaptor.forClass(String.class);
            verify(elasticsearchQueryHelper).validateAndConvertRequest(jsonSearchRequestCaptor.capture());
            return jsonSearchRequestCaptor;
        }
    }

}
