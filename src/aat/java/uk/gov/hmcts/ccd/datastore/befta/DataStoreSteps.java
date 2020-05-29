package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.java.en.Given;

public class DataStoreSteps {

    @Given("logstash has finished indexing case data")
    public void run_data_store_step() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
