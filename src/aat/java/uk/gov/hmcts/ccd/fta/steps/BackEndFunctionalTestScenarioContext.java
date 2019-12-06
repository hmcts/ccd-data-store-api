package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.java.Scenario;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.fta.data.*;

import java.util.stream.Collectors;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private Scenario scenario;
    private HttpTestData testData;
    private HttpTestData caseCreationData;
    private Long theCaseReference;
    private String theEventToken;

    public String getTheEventToken() {
        return theEventToken;
    }

    public void setTheEventToken(String theEventToken) {
        this.theEventToken = theEventToken;
    }

    private UserData theInvokingUser;
    private RequestSpecification theRequest;
    private ResponseData theResponse;

    public void initializeTestDataFor(Scenario scenario) {
        this.scenario = scenario;
        String scenarioTag = getCurrentScenarioTag();
        initializeTestDataFor(scenarioTag);
    }

    public void initializeTestDataFor(String testDataId) {
        testData = DATA_SOURCE.getDataForTestCall(testDataId);
    }

    public void initializeCaseCreationDataFor(String testDataId) {
        caseCreationData = DATA_SOURCE.getDataForTestCall(testDataId);
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

    public HttpTestData getCaseCreationData() {
        return caseCreationData;
    }

    public Long getTheCaseReference() {
        return theCaseReference;
    }

    public void setTheCaseReference(Long theCaseReference) {
        this.theCaseReference = theCaseReference;
    }

    public UserData getTheInvokingUser() {
        return theInvokingUser;
    }

    public void setTheInvokingUser(UserData theInvokingUser) {
        this.theInvokingUser = theInvokingUser;
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
