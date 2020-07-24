package uk.gov.hmcts.ccd.datastore.tests.helper.elastic;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.datastore.tests.helper.TestDataLoaderExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.util.TestUtils.withRetries;

public class ElasticsearchTestDataLoaderExtension extends TestDataLoaderExtension {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTestDataLoaderExtension.class);

    private static final String AAT_PRIVATE_INDEX_NAME = "aat_private_cases-000001";
    private static final String AAT_PRIVATE_INDEX_ALIAS = "aat_private_cases";
    private static final String AAT_PRIVATE2_INDEX_NAME = "aat_private2_cases-000001";
    private static final String AAT_PRIVATE2_INDEX_ALIAS = "aat_private2_cases";
    private static final long RETRY_POLL_DELAY_MILLIS = 1000;
    private static final long RETRY_POLL_INTERVAL_MILLIS = 1000;

    private final ElasticsearchHelper elasticsearchHelper = new ElasticsearchHelper();

    @Override
    protected void loadData() {
        withRetries(RETRY_POLL_DELAY_MILLIS, RETRY_POLL_INTERVAL_MILLIS, "ES index verification", () -> verifyIndex(AAT_PRIVATE_INDEX_ALIAS));
        withRetries(RETRY_POLL_DELAY_MILLIS, RETRY_POLL_INTERVAL_MILLIS, "ES index verification", () -> verifyIndex(AAT_PRIVATE2_INDEX_ALIAS));

        LOG.info("creating test case data");
        createCases();
    }

    public void deleteIndexesIfPresent() {
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE_INDEX_NAME);

        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE2_INDEX_NAME);
    }

    @Override
    public void close() {
        deleteIndexAndAlias(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS);
        deleteIndexAndAlias(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS);
    }

    private void createCases() {
        // create test cases in the alphabetical order of test class names
        waitUntilLogstashIndexesCaseData(elasticsearchHelper.getLogstashReadDelay());
    }

    private void deleteIndexAndAlias(String index, String indexAlias) {
        deleteIndexAlias(index, indexAlias);
        deleteIndex(index);
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

    private boolean verifyIndex(String indexAlias) {
        try {
            asElasticsearchApiUser()
                .when()
                .get(indexAlias)
                .then()
                .statusCode(200);
        } catch (AssertionError e) {
            LOG.info("Retrying Elasticsearch index api due to error: {}", e.getMessage());
            return false;
        }
        return true;
    }

}
