package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;

import java.util.UUID;

public class DataStoreSteps {

    @Before
    public void createUID() {
        String uniqueID = UUID.randomUUID().toString();
        ScenarioData.setUniqueString("string-" + uniqueID);
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
