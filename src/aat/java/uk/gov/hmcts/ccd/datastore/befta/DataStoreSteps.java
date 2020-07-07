package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import org.junit.AssumptionViolatedException;

import java.util.UUID;

import static java.util.Optional.ofNullable;

public class DataStoreSteps {

    @Before
    public void createUID() {
        String uniqueID = UUID.randomUUID().toString();
        ScenarioData.setUniqueString("string-" + uniqueID);
    }

    @Before("@elasticsearch")
    public void skipElasticSearchTestsIfNotEnabled() {
        if (!ofNullable(System.getenv("ELASTIC_SEARCH_ENABLED")).map(Boolean::valueOf).orElse(false)) {
            throw new AssumptionViolatedException("Elastic Search not Enabled");
        }
    }

    @And("a wait time of {int} seconds [{}]")
    public void waitNSecondsFor(int seconds, String string) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
