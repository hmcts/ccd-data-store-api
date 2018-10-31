package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

class ElasticsearchSearchRequest {

    static String exactMatch(String field, Object value) {
        return "{"
            + "  \"query\": {"
            + "    \"match\": {"
            + "    \"" + field + "\" : \"" + String.valueOf(value) + "\""
            + "    }"
            + "  }"
            + "}";
    }


    static String wildcardMatch(String field, Object value) {
        return "{"
            + "  \"query\": {"
            + "    \"wildcard\": {"
            + "    \"" + field + "\" : \"" + String.valueOf(value) + "\""
            + "    }"
            + "  }"
            + "}";
    }

}
