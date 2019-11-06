package uk.gov.hmcts.ccd.fta.steps;

import java.util.stream.Collectors;

import io.cucumber.core.api.Scenario;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private HttpTestData testData;
    private Scenario scenario;
    private UserData aUser;
    private Object theRequest;
    private ResponseData theResponse;

    public void initilizeTestDataFor(Scenario scenario) {
        this.scenario = scenario;
        String scenarioTag = getCurrentScenarioTag();
        testData = DATA_SOURCE.getDataForScenario(scenarioTag);
    }

    private String getCurrentScenarioTag() {
        return scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@S-"))
            .collect(Collectors.joining())
            .substring(1);
    }

    public HttpTestData getTestData() {
        return testData;
    }

    public UserData getAUser() {
        return aUser;
    }

    public void setAUser(UserData aUser) {
        this.aUser = aUser;
    }

    public void setTheRequest(Object theRequest) {
        this.theRequest = theRequest;
    }

    public Object getTheRequest() {
        return theRequest;
    }

    public ResponseData getTheResponse() {
        return theResponse;
    }

    public void setTheResponse(ResponseData theResponse) {
        this.theResponse = theResponse;
    }
}
