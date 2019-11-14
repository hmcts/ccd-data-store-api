package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.core.api.Scenario;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

import java.util.stream.Collectors;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private HttpTestData testData;
    private Scenario scenario;
    private UserData theUser;
    private RequestSpecification theRequest;
    private ResponseData theResponse;

    public void initializeTestDataFor(Scenario scenario) {
        this.scenario = scenario;
        String scenarioTag = getCurrentScenarioTag();
        testData = DATA_SOURCE.getDataForScenario(scenarioTag);
    }

    public String getCurrentScenarioTag() {
        return scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@S-"))
            .collect(Collectors.joining())
            .substring(1);
    }

    public HttpTestData getTestData() {
        return testData;
    }

    public UserData getTheUser() {
        return theUser;
    }

    public void setTheUser(UserData theUser) {
        this.theUser = theUser;
    }

    public void setTheRequest(RequestSpecification theRequest) {
        this.theRequest = theRequest;
    }

    public RequestSpecification getTheRequest() {
        return theRequest;
    }

    public ResponseData getTheResponse() {
        return theResponse;
    }

    public void setTheResponse(ResponseData theResponse) {
        this.theResponse = theResponse;
    }
}
