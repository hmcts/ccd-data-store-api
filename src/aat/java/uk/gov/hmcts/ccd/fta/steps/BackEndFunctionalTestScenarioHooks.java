package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class BackEndFunctionalTestScenarioHooks {

    private static Scenario scenario;

    public static Scenario getScenario() {
        return scenario;
    }

    @Before()
    public void prepare(Scenario scenario) {
        BackEndFunctionalTestScenarioHooks.scenario = scenario;
    }
}
