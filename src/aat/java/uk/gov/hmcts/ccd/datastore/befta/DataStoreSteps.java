package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import uk.gov.hmcts.befta.player.DefaultBackEndFunctionalTestScenarioPlayer;

import java.io.IOException;
import java.util.UUID;

public class DataStoreSteps {

    @Given("logstash has finished indexing case data")
    public void run_data_store_step() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void createUID(){
        String uniqueID = UUID.randomUUID().toString();
        ScenarioData.setUniqueString( "string-"+uniqueID);
    }

}
