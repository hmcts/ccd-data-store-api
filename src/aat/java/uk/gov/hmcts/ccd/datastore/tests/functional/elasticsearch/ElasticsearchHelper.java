package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import uk.gov.hmcts.ccd.datastore.tests.Env;

final class ElasticsearchHelper {

    private static final long DEFAULT_LOGSTASH_READ_DELAY_MILLIS = 5000;

    String getElasticsearchBaseUri() {
        return Env.require("ELASTIC_SEARCH_SCHEME") + "://" + Env.require("ELASTIC_SEARCH_HOST") + ":" + Env.require("ELASTIC_SEARCH_PORT");
    }

    Long getLogstashReadDelay() {
        try {
            return Long.valueOf(Env.require("LOGSTASH_READ_DELAY_MILLIS"));
        } catch (NullPointerException e) {
            return DEFAULT_LOGSTASH_READ_DELAY_MILLIS;
        }
    }
}
