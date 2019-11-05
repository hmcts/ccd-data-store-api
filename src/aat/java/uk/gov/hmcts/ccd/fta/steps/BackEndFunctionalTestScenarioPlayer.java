package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Test;

public class BackEndFunctionalTestScenarioPlayer {

    private BackEndFunctionalTestScenarioContext scenarioContext;

    public BackEndFunctionalTestScenarioPlayer() {
        scenarioContext = new BackEndFunctionalTestScenarioContext();
    }

    BackEndFunctionalTestScenarioContext getScenarioContext() {
        return scenarioContext;
    }

    @Test
    public void loadTestData() {
        scenarioContext.loadTestData("S-129");

    }

}
