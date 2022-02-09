package uk.gov.hmcts.ccd.domain.service.search.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GlobalSearchQueryBuilderTest {

    private static final List<String> REFERENCE_TERMS = List.of("Reference_1", "Reference_2");
    private static final List<String> JURISDICTION_TERMS = List.of("Jurisdiction_1", "Jurisdiction_2");
    private static final List<String> CASE_TYPE_TERMS = List.of("Case_Type_1", "Case_Type_2");
    private static final List<String> STATE_TERMS = List.of("State_1", "State_2");
    private static final List<String> REGION_TERMS = List.of("Region_1", "Region_2");
    private static final List<String> BASE_LOCATION_TERMS = List.of("Base_Location_1", "Base_Location_2");
    private static final List<String> OTHER_REFERENCE_TERMS = List.of("Other_Reference_1", "Other_Reference_2");

    @InjectMocks
    private GlobalSearchQueryBuilder classUnderTest;


    @Nested
    @DisplayName("GlobalSearch Query")
    class GlobalSearchQuery {

        private final ObjectMapperService objectMapperService = new DefaultObjectMapperService(new ObjectMapper());

        @DisplayName("Null Check: should return empty QueryBuilder when supplied with null request")
        @Test
        void shouldReturnEmptyBuilderForNullRequest() {

            // ARRANGE

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(null);

            // ASSERT
            assertFalse(toBoolQueryBuilder(output).hasClauses());

        }

        @DisplayName("Null Check: should return empty QueryBuilder when supplied with null SearchCriteria")
        @Test
        void shouldReturnEmptyBuilderForNullSearchCriteria() {

            // ARRANGE
            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(null);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            assertFalse(toBoolQueryBuilder(output).hasClauses());

        }

        @DisplayName("Term Filters: should add terms filter when corresponding SearchCriteria supplied")
        @Test
        void shouldAddTermsFilterWhenCorrespondingSearchCriteriaSupplied() {

            // ARRANGE
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCaseReferences(REFERENCE_TERMS);
            searchCriteria.setCcdJurisdictionIds(JURISDICTION_TERMS);
            searchCriteria.setCcdCaseTypeIds(CASE_TYPE_TERMS);
            searchCriteria.setStateIds(STATE_TERMS);
            searchCriteria.setCaseManagementRegionIds(REGION_TERMS);
            searchCriteria.setCaseManagementBaseLocationIds(BASE_LOCATION_TERMS);
            searchCriteria.setOtherReferences(OTHER_REFERENCE_TERMS);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            assertAll(
                () -> assertTermsQuery(output, GlobalSearchFields.JURISDICTION, JURISDICTION_TERMS),
                () -> assertTermsQuery(output, GlobalSearchFields.CASE_TYPE, CASE_TYPE_TERMS),
                () -> assertTermsQuery(output, GlobalSearchFields.STATE, STATE_TERMS),
                () -> assertTermsQuery(output, CaseDataPaths.REGION, REGION_TERMS),
                () -> assertTermsQuery(output, CaseDataPaths.BASE_LOCATION, BASE_LOCATION_TERMS),
                () -> assertWildcardQuery(output, CaseDataPaths.OTHER_REFERENCE_VALUE + ".keyword",
                    OTHER_REFERENCE_TERMS),
                () -> assertWildcardQuery(output, GlobalSearchFields.REFERENCE + ".keyword", REFERENCE_TERMS)
            );

            BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) output;
            assertEquals(2, Integer.valueOf(boolQueryBuilder.minimumShouldMatch()));

        }

        @DisplayName("Term Filters: should skip terms filter when corresponding SearchCriteria is empty")
        @Test
        void shouldSkipTermsFilterWhenCorrespondingSearchCriteriaIsEmpty() {

            // ARRANGE
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCaseReferences(List.of()); // i.e. empty
            searchCriteria.setCcdJurisdictionIds(List.of());
            searchCriteria.setCcdCaseTypeIds(List.of());
            searchCriteria.setStateIds(List.of());
            searchCriteria.setCaseManagementRegionIds(List.of());
            searchCriteria.setCaseManagementBaseLocationIds(List.of());
            searchCriteria.setOtherReferences(List.of());

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            assertAllTermsQuerySkipped(output);

        }

        @DisplayName("Term Filters: should skip terms filter when corresponding SearchCriteria is null")
        @Test
        void shouldSkipTermsFilterWhenCorrespondingSearchCriteriaIsNull() {

            // ARRANGE
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setCaseReferences(null);
            searchCriteria.setCcdJurisdictionIds(null);
            searchCriteria.setCcdCaseTypeIds(null);
            searchCriteria.setStateIds(null);
            searchCriteria.setCaseManagementRegionIds(null);
            searchCriteria.setCaseManagementBaseLocationIds(null);
            searchCriteria.setOtherReferences(null);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            assertAllTermsQuerySkipped(output);
        }

        @DisplayName("Parties Filter: should add parties filter when parties SearchCriteria supplied")
        @Test
        void shouldAddPartiesFilterWhenPartiesSearchCriteriaSupplied() {

            // ARRANGE
            Party party1 = new Party();
            party1.setPartyName("partyName_1");
            party1.setEmailAddress("email@example.com_1");
            party1.setAddressLine1("addressLine1_1");
            party1.setPostCode("postCode_1");
            party1.setDateOfBirth("2021-02-01");
            party1.setDateOfDeath("2021-05-06");

            Party party2 = new Party();
            party2.setPartyName("partyName_2");
            party2.setEmailAddress("email@example.com_2");
            party2.setAddressLine1("addressLine1_2");
            party2.setPostCode("postCode_2");
            party2.setDateOfBirth("2021-03-02");
            party2.setDateOfDeath("2021-07-08");

            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setParties(List.of(party1, party2));

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            List<QueryBuilder> partyQueries = getShouldQueryBuilders(output, CaseDataPaths.SEARCH_PARTIES);
            assertNotNull(partyQueries);
            assertEquals(2, partyQueries.size());

            // convert party list into a map based on partyName to allow checks without knowing order of list
            Map<String, NestedQueryBuilder> partyQueriesMap = new HashMap<>();
            partyQueries.forEach(partyQuery -> {
                if (partyQuery instanceof NestedQueryBuilder) {
                    NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) partyQuery;

                    partyQueriesMap.put(
                        getMatchPhraseValue(nestedQueryBuilder.query(), CaseDataPaths.SEARCH_PARTY_NAME + ".keyword"),
                        nestedQueryBuilder
                    );
                }
            });

            assertPartyQuery(party1, partyQueriesMap.get(party1.getPartyName()));
            assertPartyQuery(party2, partyQueriesMap.get(party2.getPartyName()));
        }

        @DisplayName("Parties Filter: should skip parties filter when parties SearchCriteria list is empty")
        @Test
        void shouldSkipPartiesFilterWhenPartiesSearchCriteriaListIsEmpty() {

            // ARRANGE
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setParties(List.of()); // i.e. empty

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            List<QueryBuilder> partyQueries = getShouldQueryBuilders(output, CaseDataPaths.SEARCH_PARTIES);
            assertNull(partyQueries);

        }

        @DisplayName("Parties Filter: should skip parties filter when parties SearchCriteria list is null")
        @Test
        void shouldSkipPartiesFilterWhenPartiesSearchCriteriaListIsNull() {

            // ARRANGE
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setParties(null);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            List<QueryBuilder> partyQueries = getShouldQueryBuilders(output, CaseDataPaths.SEARCH_PARTIES);
            assertNull(partyQueries);

        }

        @DisplayName("Parties Filter: should skip parties filter when parties SearchCriteria has no properties")
        @Test
        void shouldSkipPartiesFilterWhenAllPartySearchCriteriaValuesAreNullOrEmpty() {

            // ARRANGE
            Party partyEmptyProperties = new Party();
            partyEmptyProperties.setPartyName("");
            partyEmptyProperties.setEmailAddress("");
            partyEmptyProperties.setAddressLine1("");
            partyEmptyProperties.setPostCode("");
            partyEmptyProperties.setDateOfBirth("");
            partyEmptyProperties.setDateOfDeath("");

            Party partyNullProperties = new Party();
            partyNullProperties.setPartyName(null);
            partyNullProperties.setEmailAddress(null);
            partyNullProperties.setAddressLine1(null);
            partyNullProperties.setPostCode(null);
            partyNullProperties.setDateOfBirth(null);
            partyNullProperties.setDateOfDeath(null);

            List<Party> partyList = new ArrayList<>();
            partyList.add(partyEmptyProperties);
            partyList.add(partyNullProperties);
            partyList.add(null); // also test null object

            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setParties(partyList);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSearchCriteria(searchCriteria);

            // ACT
            QueryBuilder output = classUnderTest.globalSearchQuery(request);

            // ASSERT
            List<QueryBuilder> partyQueries = getShouldQueryBuilders(output, CaseDataPaths.SEARCH_PARTIES);
            assertNull(partyQueries);

        }

        private void assertAllTermsQuerySkipped(QueryBuilder output) {
            assertAll(
                () -> assertNull(getTermsQueryBuilder(output, GlobalSearchFields.REFERENCE)),
                () -> assertNull(getTermsQueryBuilder(output, GlobalSearchFields.JURISDICTION)),
                () -> assertNull(getTermsQueryBuilder(output, GlobalSearchFields.CASE_TYPE)),
                () -> assertNull(getTermsQueryBuilder(output, GlobalSearchFields.STATE)),
                () -> assertNull(getTermsQueryBuilder(output, CaseDataPaths.REGION)),
                () -> assertNull(getTermsQueryBuilder(output, CaseDataPaths.BASE_LOCATION)),
                () -> assertNull(getTermsQueryBuilder(output, CaseDataPaths.OTHER_REFERENCE_VALUE))
            );
        }

        private void assertTermsQuery(QueryBuilder output, String fieldName, List<String> expectedTerms) {
            TermsQueryBuilder termsQueryBuilder = getTermsQueryBuilder(output, fieldName);
            assertNotNull(termsQueryBuilder);

            List<String> actualTerms = getValuesFromTermsQueryBuilder(termsQueryBuilder);
            assertEquals(expectedTerms.size(), actualTerms.size());
            // NB: terms searches use: 'lowercase_normalizer'
            List<String> expectedTermsLowerCase
                = expectedTerms.stream().map(String::toLowerCase).collect(Collectors.toList());
            assertTrue(actualTerms.containsAll(expectedTermsLowerCase));
        }

        private void assertWildcardQuery(QueryBuilder output, String fieldName, List<String> expectedValues) {
            List<WildcardQueryBuilder> wildcardQueryBuilderList = getWildcardQueryBuilder(output, fieldName);
            assertNotNull(wildcardQueryBuilderList);
            List<String> actualValues = getValuesFromWildcardQueryBuilder(wildcardQueryBuilderList);
            assertEquals(expectedValues.size(), actualValues.size());
            assertEquals(expectedValues, actualValues);
        }

        private void assertPartyQuery(Party expectedParty, NestedQueryBuilder actualPartyQuery) {
            assertNotNull(actualPartyQuery);

            // rip it into must queries for each party property
            assertAll(
                () -> assertNestedMatchPhrase(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_NAME + ".keyword",
                    expectedParty.getPartyName()
                ),
                () -> assertNestedMatchPhrase(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_EMAIL_ADDRESS,
                    expectedParty.getEmailAddress()
                ),
                () -> assertNestedMatchPhrase(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_ADDRESS_LINE_1 + ".keyword",
                    expectedParty.getAddressLine1()
                ),
                () -> assertNestedMatchPhrase(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_POSTCODE,
                    expectedParty.getPostCode()
                ),
                () -> assertNestedRangeQueryForDate(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_DATE_OF_BIRTH,
                    expectedParty.getDateOfBirth()
                ),
                () -> assertNestedRangeQueryForDate(
                    actualPartyQuery,
                    CaseDataPaths.SEARCH_PARTY_DATE_OF_DEATH,
                    expectedParty.getDateOfDeath()
                )
            );
        }

        private void assertNestedMatchPhrase(NestedQueryBuilder nestedQueryBuilder,
                                             String path,
                                             String expectedText) {
            assertEquals(
                expectedText,
                getMatchPhraseValue(nestedQueryBuilder.query(), path)
            );
        }

        private void assertNestedRangeQueryForDate(NestedQueryBuilder nestedQueryBuilder,
                                                   String path,
                                                   String expectedDate) {

            RangeQueryBuilder rangeQueryBuilder = getRangeQueryBuilder(nestedQueryBuilder.query(), path);
            assertNotNull(rangeQueryBuilder);
            assertAll(
                () -> assertEquals(expectedDate, rangeQueryBuilder.from().toString()),
                () -> assertEquals(expectedDate, rangeQueryBuilder.to().toString())
            );
        }

        private BoolQueryBuilder toBoolQueryBuilder(QueryBuilder output) {
            assertNotNull(output);
            assertTrue(output instanceof BoolQueryBuilder);
            return (BoolQueryBuilder) output;
        }

        @SuppressWarnings("SameParameterValue")
        private List<QueryBuilder> getShouldQueryBuilders(QueryBuilder queryBuilder, String nestedPath) {
            BoolQueryBuilder boolQueryBuilder = toBoolQueryBuilder(queryBuilder);

            for (var mustQuery : boolQueryBuilder.must()) {
                // NB: should query is wrapped in a bool in case a second object query is added
                if (mustQuery instanceof BoolQueryBuilder) {
                    List<QueryBuilder> shouldQueries = ((BoolQueryBuilder)mustQuery).should();

                    // if contains a should: check if Nested
                    if (CollectionUtils.isNotEmpty(shouldQueries)) {
                        QueryBuilder firstShouldQuery = shouldQueries.get(0);
                        if (firstShouldQuery instanceof NestedQueryBuilder) {
                            if (nestedPath.equalsIgnoreCase(getNestedQueriesPathValue(firstShouldQuery))) {
                                return shouldQueries;
                            }
                        }
                    }
                }
            }

            return null;
        }

        private String getMatchPhraseValue(QueryBuilder queryBuilder, String fieldName) {
            BoolQueryBuilder boolQueryBuilder = toBoolQueryBuilder(queryBuilder);

            for (var mustQuery : boolQueryBuilder.must()) {
                if (mustQuery instanceof MatchPhraseQueryBuilder) {
                    MatchPhraseQueryBuilder matchPhraseQueryBuilder = (MatchPhraseQueryBuilder)mustQuery;

                    if (fieldName.equalsIgnoreCase(matchPhraseQueryBuilder.fieldName())) {
                        var value = matchPhraseQueryBuilder.value();
                        return value != null ? value.toString() : null;
                    }
                } else if (mustQuery instanceof WildcardQueryBuilder) {
                    WildcardQueryBuilder wildcardQueryBuilder = (WildcardQueryBuilder) mustQuery;

                    if (fieldName.equalsIgnoreCase(wildcardQueryBuilder.fieldName())) {
                        return wildcardQueryBuilder.value();
                    }
                }
            }

            return null;
        }

        private String getNestedQueriesPathValue(QueryBuilder queryBuilder) {
            // NB: cannot read nested path value so force into JSON
            JsonNode jsonNode = objectMapperService.convertStringToObject(queryBuilder.toString(), JsonNode.class);
            return jsonNode != null ? jsonNode.at("/nested/path").asText() : null;
        }

        private RangeQueryBuilder getRangeQueryBuilder(QueryBuilder queryBuilder, String fieldName) {
            BoolQueryBuilder boolQueryBuilder = toBoolQueryBuilder(queryBuilder);

            for (var mustQuery : boolQueryBuilder.must()) {
                if (mustQuery instanceof RangeQueryBuilder) {
                    RangeQueryBuilder rangeQueryBuilder = (RangeQueryBuilder)mustQuery;

                    if (fieldName.equalsIgnoreCase(rangeQueryBuilder.fieldName())) {
                        return rangeQueryBuilder;
                    }
                }
            }

            return null;
        }

        private TermsQueryBuilder getTermsQueryBuilder(QueryBuilder queryBuilder, String fieldName) {
            BoolQueryBuilder boolQueryBuilder = toBoolQueryBuilder(queryBuilder);

            for (var mustQuery : boolQueryBuilder.must()) {
                if (mustQuery instanceof TermsQueryBuilder) {
                    TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder)mustQuery;

                    if (fieldName.equalsIgnoreCase(termsQueryBuilder.fieldName())) {
                        return termsQueryBuilder;
                    }
                }
            }

            return null;
        }

        private List<WildcardQueryBuilder> getWildcardQueryBuilder(QueryBuilder queryBuilder, String fieldName) {
            BoolQueryBuilder boolQueryBuilder = toBoolQueryBuilder(queryBuilder);
            List<WildcardQueryBuilder> queries = new ArrayList<>();

            for (var mustQuery : boolQueryBuilder.should()) {
                if (mustQuery instanceof WildcardQueryBuilder) {
                    WildcardQueryBuilder wildcardQueryBuilder = (WildcardQueryBuilder)mustQuery;

                    if (fieldName.equalsIgnoreCase(wildcardQueryBuilder.fieldName())) {
                        queries.add(wildcardQueryBuilder);
                    }
                }
            }
            return queries;
        }

        private List<String> getValuesFromTermsQueryBuilder(TermsQueryBuilder termsQueryBuilder) {
            return termsQueryBuilder.values().stream().map(Object::toString).collect(Collectors.toList());
        }

        private List<String> getValuesFromWildcardQueryBuilder(List<WildcardQueryBuilder> wildcardQueryBuilderList) {
            List<String> wildcardValues = new ArrayList<>();
            for (WildcardQueryBuilder wildcardQueryBuilder : wildcardQueryBuilderList) {
                wildcardValues.add(wildcardQueryBuilder.value());
            }
            return wildcardValues;
        }
    }


    @Nested
    @DisplayName("GlobalSearch Sort")
    class GlobalSearchSort {

        @DisplayName("Null Check: should return empty sort list when supplied with null request")
        @Test
        void shouldReturnEmptySortForNullRequest() {

            // ARRANGE

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(null);

            // ASSERT
            assertTrue(output.isEmpty());

        }

        @DisplayName("Null Check: should return empty sort list when supplied with null SortCriteria list")
        @Test
        void shouldReturnEmptySortWhenNullSortCriteriaList() {

            // ARRANGE
            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(null);

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(request);

            // ASSERT
            assertTrue(output.isEmpty());

        }

        @DisplayName("Empty Check: should return empty sort list when supplied with empty SortCriteria list")
        @Test
        void shouldReturnEmptySortWhenEmptySortCriteriaList() {

            // ARRANGE
            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of()); // i.e. empty

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(request);

            // ASSERT
            assertTrue(output.isEmpty());

        }

        @DisplayName("Empty Criteria Check: should return empty sort list when SortCriteria values are null or empty")
        @Test
        void shouldReturnEmptySortWhenSortCriteriaValuesAreNullOrEmpty() {

            // ARRANGE
            SortCriteria sortCriteriaEmpty = new SortCriteria();
            sortCriteriaEmpty.setSortBy("");
            sortCriteriaEmpty.setSortDirection("");

            SortCriteria sortCriteriaNull = new SortCriteria();
            sortCriteriaNull.setSortBy(null);
            sortCriteriaNull.setSortDirection(null);

            List<SortCriteria> sortCriteriaList = new ArrayList<>();
            sortCriteriaList.add(sortCriteriaEmpty);
            sortCriteriaList.add(sortCriteriaNull);
            sortCriteriaList.add(null); // also test null object

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(sortCriteriaList);

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(request);

            // ASSERT
            assertTrue(output.isEmpty());

        }

        @DisplayName("Bad Criteria Check: should return empty sort list when supplied with Bad SortCriteria")
        @Test
        void shouldReturnEmptySortWhenBadSortCriteria() {

            // ARRANGE
            SortCriteria sortCriteriaBad = new SortCriteria();
            sortCriteriaBad.setSortBy("BAD_VALUE");
            sortCriteriaBad.setSortDirection("BAD_VALUE");

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of(sortCriteriaBad));

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(request);

            // ASSERT
            assertTrue(output.isEmpty());

        }

        @DisplayName("Many Sort Criteria: should return sort list when supplied with valid SortCriteria")
        @Test
        void shouldReturnManySortForManySortCriteria() {

            // ARRANGE
            SortCriteria sortCriteria1 = new SortCriteria();
            sortCriteria1.setSortBy(GlobalSearchSortByCategory.CASE_NAME.getCategoryName());
            sortCriteria1.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());

            SortCriteria sortCriteria2 = new SortCriteria();
            sortCriteria2.setSortBy(GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME.getCategoryName());
            sortCriteria2.setSortDirection(GlobalSearchSortDirection.DESCENDING.name());

            SortCriteria sortCriteria3 = new SortCriteria();
            sortCriteria3.setSortBy(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName());
            sortCriteria3.setSortDirection(null); // i.e. allow it to default

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of(sortCriteria1, sortCriteria2, sortCriteria3));

            // ACT
            List<FieldSortBuilder> output = classUnderTest.globalSearchSort(request);

            // ASSERT
            assertEquals(3, output.size());
            // :: NB: order of sort criteria should be preserved
            assertAll(
                () -> assertSortCriteria(
                    GlobalSearchSortByCategory.CASE_NAME,
                    SortOrder.ASC,
                    output.get(0)
                ),
                () -> assertSortCriteria(
                    GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME,
                    SortOrder.DESC,
                    output.get(1)
                ),
                () -> assertSortCriteria(
                    GlobalSearchSortByCategory.CREATED_DATE,
                    SortOrder.ASC, // i.e. defaulted
                    output.get(2)
                )
            );
        }

        private void assertSortCriteria(GlobalSearchSortByCategory expectedCategory,
                                        SortOrder expectedSortOrder,
                                        FieldSortBuilder actualSortBuilder) {
            assertAll(
                () -> assertEquals(expectedCategory.getField(), actualSortBuilder.getFieldName()),
                () -> assertEquals(expectedSortOrder, actualSortBuilder.order())
            );
        }

    }


    @Nested
    @DisplayName("GlobalSearch Fields")
    class GlobalSearchDataFields {

        @DisplayName("Source fields: should return non empty list of source fields")
        @Test
        void shouldReturnNonEmptyGlobalSearchSourceFields() {

            // ARRANGE

            // ACT
            ArrayNode output = classUnderTest.globalSearchSourceFields();

            // ASSERT
            assertNotNull(output);
            assertFalse(output.isEmpty());

        }

        @DisplayName("SupplementaryData fields: should return non empty list of SupplementaryData fields")
        @Test
        void shouldReturnNonEmptyGlobalSearchSupplementaryDataFields() {

            // ARRANGE

            // ACT
            ArrayNode output = classUnderTest.globalSearchSupplementaryDataFields();

            // ASSERT
            assertNotNull(output);
            assertFalse(output.isEmpty());

        }

    }

}
