package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE2_CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE_CASE_TYPE;

import com.google.common.annotations.VisibleForTesting;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

public abstract class ElasticsearchBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTestDataLoaderExtension.class);
    public static final String EXACT_MATCH_TEST_REFERENCE = TestData.uniqueReference();

    @VisibleForTesting
    static final String CASE_DATA_FIELD_PREFIX = "data.";
    @VisibleForTesting
    static final String RESPONSE_CASE_DATA_FIELDS_PREFIX = "case_data.";
    @VisibleForTesting
    static final String CASE_ID = "id";
    @VisibleForTesting
    static final String ES_FIELD_CASE_TYPE = "case_type_id";
    @VisibleForTesting
    static final String ES_FIELD_STATE = "state";
    @VisibleForTesting
    static final String ES_FIELD_CASE_REFERENCE = "reference";
    @VisibleForTesting
    static final String ES_FIELD_EMAIL_ID = "EmailField";
    @VisibleForTesting
    static final String ES_FIELD_TEXT_ALIAS = "alias.TextFieldAlias";
    @VisibleForTesting
    static final String ES_FIELD_NUMBER_ALIAS = "alias.NumberFieldAlias";
    @VisibleForTesting
    static final String ES_FIELD_EMAIL_ALIAS = "alias.EmailFieldAlias";

    private static final String CASE_TYPE_ID_PARAM = "ctid";
    private static final String CASE_SEARCH_API = "/searchCases";

    final TestData testData = TestData.getInstance();

    ElasticsearchBaseTest(AATHelper aat) {
        super(aat);
    }

    public static void assertElasticsearchEnabled() {
        // stop execution of these tests if Elasticsearch is not enabled
        LOG.info("ELASTIC_SEARCH_ENABLED: {}", System.getenv("ELASTIC_SEARCH_ENABLED"));
        boolean elasticsearchEnabled = ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
        assumeTrue(elasticsearchEnabled, () -> "Ignoring Elasticsearch tests, variable ELASTIC_SEARCH_ENABLED not set");
    }

    @VisibleForTesting
    ValidatableResponse searchCase(Supplier<RequestSpecification> requestSpecification, String jsonSearchRequest, String... caseTypes) {
        return requestSpecification.get()
            .given()
            .log()
            .body()
            .queryParam(CASE_TYPE_ID_PARAM, String.join(",", caseTypes))
            .contentType(ContentType.JSON)
            .body(jsonSearchRequest)
            .when()
            .post(CASE_SEARCH_API)
            .then()
            .statusCode(200);
    }

    @VisibleForTesting
    ValidatableResponse searchCase(Supplier<RequestSpecification> requestSpecification, String jsonSearchRequest) {
        return searchCase(requestSpecification, jsonSearchRequest, AAT_PRIVATE_CASE_TYPE);
    }

    @VisibleForTesting
    void assertSingleCaseReturned(ValidatableResponse response) {
        assertCaseListSizeInResponse(response, 1);
    }

    @VisibleForTesting
    void assertCaseListSizeInResponse(ValidatableResponse response, int expectedSize) {
        response.body("cases.size()", is(expectedSize));
    }

    @VisibleForTesting
    void assertNoCaseReturned(ValidatableResponse response) {
        response.body("cases.size()", is(0));
    }

    @VisibleForTesting
    ValidatableResponse searchAcrossCaseTypes(Supplier<RequestSpecification> asUser, String field, Object value) {
        String jsonSearchRequest = ElasticsearchSearchRequest.exactMatch(field, value);
        return searchCase(asUser, jsonSearchRequest, AAT_PRIVATE_CASE_TYPE, AAT_PRIVATE2_CASE_TYPE);
    }

    @VisibleForTesting
    void assertCaseReferencesInResponse(ValidatableResponse response, Object... values) {
        assertCaseListSizeInResponse(response, 2);
        assertField(response, CASE_ID, values);
    }

    @VisibleForTesting
    void assertField(ValidatableResponse response, String field, Object... expectedValues) {
        response.body("cases." + field, hasItems(expectedValues));
    }

}
