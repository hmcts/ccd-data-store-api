package uk.gov.hmcts.ccd.datastore.tests.util;

import io.cucumber.core.api.Scenario;
import cucumber.api.java.Before;

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
