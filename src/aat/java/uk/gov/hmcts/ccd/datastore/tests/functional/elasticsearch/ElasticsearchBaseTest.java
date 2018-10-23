package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import java.io.File;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;

abstract class ElasticsearchBaseTest extends BaseTest {

    static final String AAT_SEARCH_CASE_TYPE = "AAT_SEARCH";
    private static final String DEFINITION_FILE = "src/aat/resources/CCD_CNP_27.xlsx";

    ElasticsearchBaseTest(AATHelper aat) {
        super(aat);
    }

    void assertElasticsearchEnabled() {
        // stop execution of these tests if Elasticsearch is not enabled
        boolean elasticsearchEnabled = ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false);
        assumeTrue(elasticsearchEnabled, () -> "Ignoring Elasticsearch tests, variable ELASTIC_SEARCH_ENABLED not set");
    }

    void importDefinition() {
        asAutoTestImporter()
            .given()
            .multiPart(new File(DEFINITION_FILE))
            .expect()
            .statusCode(201)
            .when()
            .post("/import");
    }

    ValidatableResponse searchCase(String jsonSearchRequest) {
        return asAutoTestCaseworker(false).get()
            .given()
            .log()
            .body()
            .queryParam("ctid", AAT_SEARCH_CASE_TYPE)
            .contentType(ContentType.JSON)
            .body(jsonSearchRequest)
            .when()
            .post("/searchCases")
            .then()
            .statusCode(200);
    }

    void deleteIndexAndAlias(String indexName, String indexAlias) {
        deleteIndexAlias(indexName, indexAlias);
        deleteIndex(indexName);
    }

    private void deleteIndexAlias(String indexName, String indexAlias) {
        asElasticsearchApiUser()
            .when()
            .delete(indexName + "/_alias/" + indexAlias)
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
                                     .setBaseUri(aat.getElasticsearchBaseUri())
                                     .build());
    }

}
