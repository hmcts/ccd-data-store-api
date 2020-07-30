package uk.gov.hmcts.ccd.datastore.befta.es;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public class ElasticsearchTestDataLoaderExtension {

    private static final String AAT_PRIVATE_INDEX_NAME = "aat_private_cases-000001";
    private static final String AAT_PRIVATE_INDEX_ALIAS = "aat_private_cases";
    private static final String AAT_PRIVATE2_INDEX_NAME = "aat_private2_cases-000001";
    private static final String AAT_PRIVATE2_INDEX_ALIAS = "aat_private2_cases";

    private final ElasticsearchHelper elasticsearchHelper = new ElasticsearchHelper();

    public void deleteIndexesIfPresent() {
        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE_INDEX_NAME, AAT_PRIVATE_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE_INDEX_NAME);

        asElasticsearchApiUser().when().delete(getCaseIndexAliasApi(AAT_PRIVATE2_INDEX_NAME, AAT_PRIVATE2_INDEX_ALIAS));
        asElasticsearchApiUser().when().delete(AAT_PRIVATE2_INDEX_NAME);
    }

    private RequestSpecification asElasticsearchApiUser() {
        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(elasticsearchHelper.getElasticsearchBaseUri())
                                     .build());
    }

    private String getCaseIndexAliasApi(String indexName, String indexAlias) {
        return indexName + "/_alias/" + indexAlias;
    }

}
