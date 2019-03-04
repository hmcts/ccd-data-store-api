package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

@ExtendWith(ElasticsearchTestDataLoaderExtension.class)
public class ElasticsearchCrossCaseTypeSearchTest extends ElasticsearchBaseTest {

    public static final String TEXT_FIELD_VALUE = TestData.uniqueReference();
    public static final String AAT_PRIVATE_CROSS_CASE_TYPE_SEARCH_REFERENCE = TestData.uniqueReference();
    public static final String AAT_PRIVATE2_CROSS_CASE_TYPE_SEARCH_REFERENCE = TestData.uniqueReference();

    ElasticsearchCrossCaseTypeSearchTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void setUp() {
        assertElasticsearchEnabled();
    }

    @Nested
    @DisplayName("Cross case type search using alias")
    class SearchByAlias {

        @Test
        @DisplayName("should return cases across multiple case types for match on search alias field")
        void shouldReturnCases() {
            ValidatableResponse response = searchAcrossCaseTypes(asPrivateCrossCaseTypeCaseworker(false), ES_FIELD_TEXT_ALIAS, TEXT_FIELD_VALUE);

            assertCaseReferencesInResponse(response,
                                           testData.get(AAT_PRIVATE_CROSS_CASE_TYPE_SEARCH_REFERENCE),
                                           testData.get(AAT_PRIVATE2_CROSS_CASE_TYPE_SEARCH_REFERENCE));
        }

    }

    @Nested
    @DisplayName("Cross case type case data filter in response")
    class FilterByAlias {

        @Test
        @DisplayName("should return metadata and case data when source filter with aliases is requested in the search")
        void shouldReturnMetadataAndCaseData() {
            String jsonSearchRequest = ElasticsearchSearchRequest.exactMatchWithSourceFilter(ES_FIELD_TEXT_ALIAS, TEXT_FIELD_VALUE, ES_FIELD_TEXT_ALIAS,
                                                                                             ES_FIELD_NUMBER_ALIAS);
            ValidatableResponse response = searchCase(asPrivateCrossCaseTypeCaseworker(false), jsonSearchRequest, AATCaseType.AAT_PRIVATE_CASE_TYPE,
                                                      AATCaseType.AAT_PRIVATE2_CASE_TYPE);

            assertCaseListSizeInResponse(response, 2);
            response.body("cases[0].keySet()", hasSize(greaterThan(1)));
            response.body("cases[0].case_data.keySet()", hasSize(2));
            response.body("cases[1].case_data.keySet()", hasSize(2));
            response.body("cases[0].case_data", hasKey("TextFieldAlias"));
            response.body("cases[0].case_data", hasKey("NumberFieldAlias"));
        }

        @Test
        @DisplayName("should return metadata only when source filter is not requested")
        void shouldReturnMetadataOnly() {
            ValidatableResponse response = searchAcrossCaseTypes(asPrivateCrossCaseTypeCaseworker(false), ES_FIELD_TEXT_ALIAS, TEXT_FIELD_VALUE);

            assertCaseListSizeInResponse(response, 2);
            response.body("cases[0].keySet()", hasSize(greaterThan(1)));
            response.body("cases[0].case_data.keySet()", emptyIterable());
            response.body("cases[1].case_data.keySet()", emptyIterable());
        }

    }

}
