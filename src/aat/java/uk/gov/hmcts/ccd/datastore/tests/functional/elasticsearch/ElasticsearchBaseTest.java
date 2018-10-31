package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE_CASE_TYPE;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.TestData;

abstract class ElasticsearchBaseTest extends BaseTest {

    static final String CASE_DATA_FIELD_PREFIX = "data.";
    static final String RESPONSE_CASE_DATA_FIELDS_PREFIX = "case_data.";
    static final String ES_FIELD_STATE = "state";
    static final String ES_FIELD_CASE_REFERENCE = "reference";
    static final String ES_FIELD_EMAIL_ID = "EmailField";
    static final String CASE_ID = "id";

    private static final String CASE_TYPE_ID_PARAM = "ctid";
    private static final String CASE_SEARCH_API = "/searchCases";

    final TestData testData = TestData.getInstance();

    ElasticsearchBaseTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void assertElasticsearchEnabled() {
        // stop execution of these tests if Elasticsearch is not enabled
        boolean elasticsearchEnabled = ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
        assumeTrue(elasticsearchEnabled, () -> "Ignoring Elasticsearch tests, variable ELASTIC_SEARCH_ENABLED not set");
    }

    ValidatableResponse searchCase(Supplier<RequestSpecification> requestSpecification, String jsonSearchRequest) {
        return requestSpecification.get()
            .given()
            .log()
            .body()
            .queryParam(CASE_TYPE_ID_PARAM, AAT_PRIVATE_CASE_TYPE)
            .contentType(ContentType.JSON)
            .body(jsonSearchRequest)
            .when()
            .post(CASE_SEARCH_API)
            .then()
            .statusCode(200);
    }

    void assertSingleCaseReturned(ValidatableResponse response) {
        response.body("cases.size()", is(1));
    }

    void assertNoCaseReturned(ValidatableResponse response) {
        response.body("cases.size()", is(0));
    }

    void assertField(ValidatableResponse response, String field, Object expectedValue) {
        response.body("cases[0]." + field, is(expectedValue));
    }
}
