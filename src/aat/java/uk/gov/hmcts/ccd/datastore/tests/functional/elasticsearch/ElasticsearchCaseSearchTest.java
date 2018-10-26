package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import static org.hamcrest.CoreMatchers.is;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase.DATE_TIME;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase.TEXT;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchCaseSearchTest extends ElasticsearchBaseTest {

    ElasticsearchCaseSearchTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    void setUp() {
        assertElasticsearchEnabled();
        importDefinition();
        createCases();
    }

    @Nested
    @DisplayName("Search updated case")
    class SearchUpdatedCase {

        @Test
        @DisplayName("should return updated case on search")
        void shouldReturnUpdatedCaseOnSearch() {
            String jsonSearchRequest = "{"
                + "  \"query\": {"
                + "    \"match\": {"
                + "    \"state\" : \"" + State.IN_PROGRESS + "\""
                + "    }"
                + "  }"
                + "}";

            ValidatableResponse response = searchCaseAsPrivateCaseWorker(jsonSearchRequest);

            response.body("cases.size()", is(1));
            response.body("cases[0].state", is(State.IN_PROGRESS));
        }
    }

    @Nested
    @DisplayName("Exact match")
    class ExactMatch {

        @Nested
        @DisplayName("text field")
        class TextField {

            @Test
            @DisplayName("should return case for exact match on a text field")
            void shouldReturnCaseForExactMatchOnTextField() {
                searchCaseForExactMatchAndVerifyResponse("TextField", TEXT);
            }
        }

        @Nested
        @DisplayName("date time field")
        class DateTimeField {

            @Test
            @DisplayName("should return case for exact match on a date time field")
            void shouldReturnCaseForExactMatchOnDateTimeField() {
                searchCaseForExactMatchAndVerifyResponse("DateTimeField", DATE_TIME);
            }
        }

        private void searchCaseForExactMatchAndVerifyResponse(String field, String value) {
            String jsonSearchRequest = createExactMatchSearchRequest(field, value);

            ValidatableResponse response = searchCaseAsPrivateCaseWorker(jsonSearchRequest);

            verifyExactMatchResponse(response, field, value);
        }

        private String createExactMatchSearchRequest(String field, String value) {
            return "{"
                + "  \"query\": {"
                + "    \"match\": {"
                + "    \"data." + field + "\" : \"" + value + "\""
                + "    }"
                + "  }"
                + "}";
        }

        private void verifyExactMatchResponse(ValidatableResponse response, String field, String expectedValue) {
            response.body("cases.size()", is(1));
            response.body("cases[0].case_data." + field, is(expectedValue));
        }
    }

    @Nested
    @DisplayName("Wildcard")
    class Wildcard {

        @Nested
        @DisplayName("text field")
        class TextField {

            @Test
            @DisplayName("should return case matching wildcard expression on a text field")
            void shouldReturnCaseForWildcardMatchOnTextField() {
                String wildcardExpr = TEXT.substring(0, 3).toLowerCase() + "*";
                searchCaseByWildcardAndVerifyResponse("TextField", wildcardExpr, TEXT);
            }
        }

        private void searchCaseByWildcardAndVerifyResponse(String field, String wildcardExpr, String expectedValue) {
            String jsonSearchRequest = createWildcardSearchRequest(field, wildcardExpr);

            ValidatableResponse response = searchCaseAsPrivateCaseWorker(jsonSearchRequest);

            verifyWildcardMatchResponse(response, field, expectedValue);
        }

        private String createWildcardSearchRequest(String field, String wildcardExpr) {
            return "{"
                + "  \"query\": {"
                + "    \"wildcard\": {"
                + "    \"data." + field + "\" : \"" + wildcardExpr + "\""
                + "    }"
                + "  }"
                + "}";
        }

        private void verifyWildcardMatchResponse(ValidatableResponse response, String field, String expectedValue) {
            response.body("cases.size()", is(1));
            response.body("cases[0].case_data." + field, is(expectedValue));
        }
    }

    @AfterAll
    void cleanUp() {
        deleteIndexAndAlias();
    }

    private void createCases() {
        createCaseAndProgressState(asPrivateCaseworker(true));
        createCase(asPrivateCaseworker(true), FullCase.build());

        // wait until logstash reads the case data
        sleep(aat.getLogstashReadDelay());
    }

}
