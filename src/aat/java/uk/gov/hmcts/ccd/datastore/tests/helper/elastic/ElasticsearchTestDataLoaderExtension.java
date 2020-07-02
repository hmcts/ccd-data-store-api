package uk.gov.hmcts.ccd.datastore.tests.helper.elastic;

import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE2_CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.AAT_PRIVATE_CASE_TYPE;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.JURISDICTION_AUTOTEST2;
import static uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch.ElasticsearchBaseTest.assertElasticsearchEnabled;
import static uk.gov.hmcts.ccd.datastore.tests.util.TestUtils.withRetries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch.ElasticSearchTextFieldTest;
import uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch.ElasticsearchCaseSearchSecurityTest;
import uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch.ElasticsearchCrossCaseTypeSearchSecurityTest;
import uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch.ElasticsearchCrossCaseTypeSearchTest;
import uk.gov.hmcts.ccd.datastore.tests.helper.TestDataLoaderExtension;

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
        assertElasticsearchEnabled();

        withRetries(RETRY_POLL_DELAY_MILLIS, RETRY_POLL_INTERVAL_MILLIS, "ES index verification", () -> verifyIndex(AAT_PRIVATE_INDEX_ALIAS));
        withRetries(RETRY_POLL_DELAY_MILLIS, RETRY_POLL_INTERVAL_MILLIS, "ES index verification", () -> verifyIndex(AAT_PRIVATE2_INDEX_ALIAS));

        LOG.info("creating test case data");
        createCases();
    }

    public void deleteIndexesIfPresent() {
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE_INDEX_NAME);

        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE2_INDEX_ALIAS);
    }

    @Override
    public void close() {
        deleteIndexAndAlias(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS);
        deleteIndexAndAlias(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS);
    }

    private void createCases() {
        // create test cases in the alphabetical order of test class names
        createCasesForCaseSearchSecurityTest();
        createCasesForTextSearchTest();
        createCasesForCrossCaseTypeSearchTest();
        createCasesForCrossCaseTypeSecurityTest();
        waitUntilLogstashIndexesCaseData(elasticsearchHelper.getLogstashReadDelay());
    }

    private void createCasesForCaseSearchSecurityTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_TYPE_SECURITY_TEST_REFERENCE,
                     createCase(asPrivateCaseworker(true), AAT_PRIVATE_CASE_TYPE, AATCaseBuilder.EmptyCase.build()));
        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_STATE_SECURITY_TEST_REFERENCE,
                     createCase(asPrivateCaseworker(true), AAT_PRIVATE_CASE_TYPE, AATCaseBuilder.EmptyCase.build()));
        testData.put(ElasticsearchCaseSearchSecurityTest.CASE_FIELD_SECURITY_TEST_REFERENCE,
                     createCase(asRestrictedCaseworker(true),
                                AAT_PRIVATE_CASE_TYPE,
                                AATCaseType.CaseData.builder().emailField(ElasticsearchCaseSearchSecurityTest.EMAIL_ID_VALUE).build()));
    }

    private void createCasesForTextSearchTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticSearchTextFieldTest.SEARCH_UPDATED_CASE_TEST_REFERENCE,
                     createCaseAndProgressState(asPrivateCaseworker(true), AAT_PRIVATE_CASE_TYPE));
        testData.put(ElasticSearchTextFieldTest.EXACT_MATCH_TEST_REFERENCE,
                     createCase(asPrivateCaseworker(true), AAT_PRIVATE_CASE_TYPE, AATCaseBuilder.FullCase.build()));
    }

    private void createCasesForCrossCaseTypeSearchTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticsearchCrossCaseTypeSearchTest.AAT_PRIVATE_CROSS_CASE_TYPE_SEARCH_REFERENCE,
                     createCase(asPrivateCaseworker(true),
                                AAT_PRIVATE_CASE_TYPE,
                                AATCaseType.CaseData.builder()
                                    .textField(ElasticsearchCrossCaseTypeSearchTest.TEXT_FIELD_VALUE)
                                    .numberField("1")
                                    .build()));

        testData.put(ElasticsearchCrossCaseTypeSearchTest.AAT_PRIVATE2_CROSS_CASE_TYPE_SEARCH_REFERENCE,
                     createCase(asPrivateCrossCaseTypeCaseworker(true),
                                JURISDICTION_AUTOTEST2,
                                AAT_PRIVATE2_CASE_TYPE,
                                AATCaseType.CaseData.builder()
                                    .textField(ElasticsearchCrossCaseTypeSearchTest.TEXT_FIELD_VALUE)
                                    .numberField("2")
                                    .build()));
    }

    private void createCasesForCrossCaseTypeSecurityTest() {
        TestData testData = TestData.getInstance();

        testData.put(ElasticsearchCrossCaseTypeSearchSecurityTest.AAT_PRIVATE_SECURITY_TEST_REFERENCE,
                     createCase(asPrivateCrossCaseTypeCaseworker(true),
                                AAT_PRIVATE_CASE_TYPE,
                                AATCaseType.CaseData.builder()
                                    .numberField(ElasticsearchCrossCaseTypeSearchSecurityTest.NUMBER_FIELD_VALUE)
                                    .emailField(ElasticsearchCrossCaseTypeSearchSecurityTest.EMAIL_ID_VALUE)
                                    .build()));

        testData.put(ElasticsearchCrossCaseTypeSearchSecurityTest.AAT_PRIVATE2_SECURITY_TEST_REFERENCE,
                     createCase(asPrivateCrossCaseTypeCaseworker(true),
                                JURISDICTION_AUTOTEST2,
                                AAT_PRIVATE2_CASE_TYPE,
                                AATCaseType.CaseData.builder()
                                    .numberField(ElasticsearchCrossCaseTypeSearchSecurityTest.NUMBER_FIELD_VALUE)
                                    .emailField(ElasticsearchCrossCaseTypeSearchSecurityTest.EMAIL_ID_VALUE)
                                    .build()));
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
