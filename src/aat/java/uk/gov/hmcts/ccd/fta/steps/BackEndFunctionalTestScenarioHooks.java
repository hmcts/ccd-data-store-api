package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;

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
