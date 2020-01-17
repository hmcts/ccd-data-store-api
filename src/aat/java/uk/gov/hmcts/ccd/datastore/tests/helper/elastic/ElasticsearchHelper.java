package uk.gov.hmcts.ccd.datastore.tests.helper.elastic;

import uk.gov.hmcts.ccd.datastore.tests.Env;

final class ElasticsearchHelper {

    private static final long DEFAULT_LOGSTASH_READ_DELAY_MILLIS = 5000;

    String getElasticsearchBaseUri() {
        return Env.require("ELASTIC_SEARCH_HOSTS");
    }

    Long getLogstashReadDelay() {
        try {
            return Long.valueOf(Env.require("LOGSTASH_READ_DELAY_MILLIS"));
        } catch (NullPointerException e) {
            return DEFAULT_LOGSTASH_READ_DELAY_MILLIS;
        }
    }
}
