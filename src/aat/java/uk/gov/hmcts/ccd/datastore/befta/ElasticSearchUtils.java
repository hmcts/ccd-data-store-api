package uk.gov.hmcts.ccd.datastore.befta;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.Env;

import static org.hamcrest.Matchers.equalTo;

public class ElasticSearchUtils {

    private static final String AAT_PRIVATE_INDEX_NAME = "aat_private_cases-000001";
    private static final String AAT_PRIVATE_INDEX_ALIAS = "aat_private_cases";
    private static final String AAT_PRIVATE2_INDEX_NAME = "aat_private2_cases-000001";
    private static final String AAT_PRIVATE2_INDEX_ALIAS = "aat_private2_cases";
    private static final String BEFTA_JURISDICTION_2_CASETYPE_1_NAME = "befta_casetype_2_1_cases-000001";
    private static final String BEFTA_JURISDICTION_2_CASETYPE_1_ALIAS = "befta_casetype_2_1_cases";

    private static final String FT_COMPLEXCOLLECTIONCOMPLEX_INDEX_NAME = "ft_complexcollectioncomplex_cases-000001";
    private static final String FT_COMPLEXCOLLECTIONCOMPLEX_INDEX_ALIAS = "ft_complexcollectioncomplex_cases";


    String getElasticsearchBaseUri() {
        return Env.require("ELASTIC_SEARCH_HOSTS");
    }

    private String getCaseIndexAliasApi(String indexName, String indexAlias) {
        return indexName + "/_alias/" + indexAlias;
    }

    public void deleteIndexesIfPresent() {
        System.out.println("deleting CCD ES Test Indexes");
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE_INDEX_NAME);
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE2_INDEX_NAME);
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(BEFTA_JURISDICTION_2_CASETYPE_1_NAME, BEFTA_JURISDICTION_2_CASETYPE_1_ALIAS));
        asElasticsearchApiUser().when().delete(BEFTA_JURISDICTION_2_CASETYPE_1_NAME);
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(FT_COMPLEXCOLLECTIONCOMPLEX_INDEX_NAME, FT_COMPLEXCOLLECTIONCOMPLEX_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(FT_COMPLEXCOLLECTIONCOMPLEX_INDEX_NAME);
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
            .setBaseUri(getElasticsearchBaseUri())
            .build());
    }

}
