package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.core.api.Scenario;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;

import java.util.stream.Collectors;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private HttpTestData testData;


    public boolean loadTestData() {
        String scenarioTag = getCurrentScenarioTag();
        testData = DATA_SOURCE.getDataForScenario(scenarioTag);
        System.out.println(scenarioTag);
        return testData != null;
    }

    private String getCurrentScenarioTag() {
        Scenario scenario = BackEndFunctionalTestScenarioHooks.getScenario();
        return scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@S-"))
            .collect(Collectors.joining())
            .substring(1);
    }
}
