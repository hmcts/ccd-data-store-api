package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AssumptionViolatedException;
import uk.gov.hmcts.ccd.datastore.tests.Env;

import java.util.Map;

/**
 * Test-only Elasticsearch controls for exercising Logstash retry behaviour.
 *
 * <p>The test is deliberately opt-in: changing index write settings affects every Logstash worker
 * sharing the AAT Elasticsearch cluster. The {@code @After} hook always restores the setting.
 */
public class LogstashOutageSteps {

    private static final String PRIVATE_CASE_INDEX = "aat_private_cases*";
    private boolean writesBlocked;

    @Before("@logstash-outage")
    public void requireExplicitOutageTestOptIn() {
        if (!Boolean.parseBoolean(System.getenv("LOGSTASH_OUTAGE_FTA_ENABLED"))) {
            throw new AssumptionViolatedException(
                "Logstash outage tests are disabled. Set LOGSTASH_OUTAGE_FTA_ENABLED=true to run them."
            );
        }
    }

    @After("@logstash-outage")
    public void restoreElasticsearchWrites() {
        if (writesBlocked) {
            setWriteBlock(false);
        }
    }

    @Given("Elasticsearch writes are blocked for the AAT private-case index")
    public void blockElasticsearchWrites() {
        setWriteBlock(true);
        writesBlocked = true;
    }

    @Given("Elasticsearch writes are restored for the AAT private-case index")
    public void restoreWrites() {
        setWriteBlock(false);
        writesBlocked = false;
    }

    private void setWriteBlock(boolean blockWrites) {
        RestAssured.given()
            .baseUri(Env.require("ELASTIC_SEARCH_HOSTS"))
            .contentType(ContentType.JSON)
            .body(Map.of("index.blocks.write", blockWrites))
            .when()
            .put("/" + PRIVATE_CASE_INDEX + "/_settings")
            .then()
            .statusCode(200);
    }
}
