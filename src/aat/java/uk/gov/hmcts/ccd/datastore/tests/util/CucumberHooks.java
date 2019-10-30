package uk.gov.hmcts.ccd.datastore.tests.util;

import cucumber.api.java.Before;
import io.cucumber.core.api.Scenario;

public class CucumberHooks {

    private static Scenario scenario;

    public static Scenario getScenario() {
        return scenario;
    }

    @Before()
    public void prepare(Scenario scenario) {
        this.scenario = scenario;
    }
}
