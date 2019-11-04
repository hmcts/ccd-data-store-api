package uk.gov.hmcts.ccd.fta.steps;

public class BackEndFunctionalTestScenarioPlayer {

    private BackEndFunctionalTestScenarioContext scenarioContext;

    public BackEndFunctionalTestScenarioPlayer() {
        scenarioContext = new BackEndFunctionalTestScenarioContext();
    }

    BackEndFunctionalTestScenarioContext getScenarioContext() {
        return scenarioContext;
    }

}
