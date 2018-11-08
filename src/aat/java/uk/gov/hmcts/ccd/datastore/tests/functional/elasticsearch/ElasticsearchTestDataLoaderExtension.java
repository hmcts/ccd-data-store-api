package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.TestDataLoaderExtension;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;

public class ElasticsearchTestDataLoaderExtension extends TestDataLoaderExtension {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTestDataLoaderExtension.class);

    private static final String CASE_INDEX_NAME = "aat_private_cases-000001";
    private static final String CASE_INDEX_ALIAS = "aat_private_cases";

    private final ElasticsearchHelper elasticsearchHelper = new ElasticsearchHelper();

    @Override
    protected void loadData() {
        assertElasticsearchEnabled();

        LOG.info("importing definition");
        importDefinition();

        LOG.info("creating test case data");
        createCases();
    }

    private void assertElasticsearchEnabled() {
        // stop execution of these tests if Elasticsearch is not enabled
        LOG.info("ELASTIC_SEARCH_ENABLED: {}", System.getenv("ELASTIC_SEARCH_ENABLED"));
        boolean elasticsearchEnabled = ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
        assumeTrue(elasticsearchEnabled, () -> "Ignoring Elasticsearch tests, variable ELASTIC_SEARCH_ENABLED not set");
    }

    @Override
    public void close() {
        LOG.info("Deleting index and alias");
        deleteIndexAndAlias();
    }

    private void createCases() {
        // create test cases in the alphabetical order of test class names
        createCasesForCaseSearchSecurityTest();
        createCasesForCaseSearchTest();
        waitUntilLogstashIndexesCaseData(elasticsearchHelper.getLogstashReadDelay());
    }

    private void createCasesForCaseSearchSecurityTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_TYPE_SECURITY_TEST_REFERENCE,
                     createCase(asPrivateCaseworker(true), AATCaseBuilder.EmptyCase.build()));
        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_STATE_SECURITY_TEST_REFERENCE,
                     createCaseAndProgressState(asPrivateCaseworker(true)));
        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_FIELD_SECURITY_TEST_REFERENCE,
                     createCase(asRestrictedCaseworker(true),
                                AATCaseType.CaseData.builder().emailField(ElasticsearchCaseSearchSecurityTest.EMAIL_ID_VALUE).build()));
    }

    private void createCasesForCaseSearchTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticsearchCaseSearchTest.SEARCH_UPDATED_CASE_TEST_REFERENCE,
                     createCaseAndProgressState(asPrivateCaseworker(true)));
        testData.put(ElasticsearchCaseSearchTest.EXACT_MATCH_TEST_REFERENCE,
                     createCase(asPrivateCaseworker(true), AATCaseBuilder.FullCase.build()));
    }

    private void deleteIndexAndAlias() {
        deleteIndexAlias(CASE_INDEX_NAME, CASE_INDEX_ALIAS);
        deleteIndex(CASE_INDEX_NAME);
    }

    private void deleteIndexAlias(String indexName, String indexAlias) {
        asElasticsearchApiUser()
            .when()
            .delete(getCaseIndexAliasApi(indexName, indexAlias))
            .then()
            .statusCode(200)
            .body("acknowledged", equalTo(true));
    }

    private void deleteIndex(String indexName) {
        asElasticsearchApiUser()
            .when()
            .delete(indexName)
            .then()
            .statusCode(200)
            .body("acknowledged", equalTo(true));
    }

    private RequestSpecification asElasticsearchApiUser() {
        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(elasticsearchHelper.getElasticsearchBaseUri())
                                     .build());
    }

    private String getCaseIndexAliasApi(String indexName, String indexAlias) {
        return indexName + "/_alias/" + indexAlias;
    }

    private void waitUntilLogstashIndexesCaseData(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
