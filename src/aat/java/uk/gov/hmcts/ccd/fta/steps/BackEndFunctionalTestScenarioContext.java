package uk.gov.hmcts.ccd.fta.steps;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.cucumber.java.Scenario;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private Scenario scenario;
    protected HttpTestData testData;
    private HttpTestData caseCreationData;
    private Long theCaseReference;
    private UserData theInvokingUser;
    private RequestSpecification theRequest;
    private ResponseData theResponse;

    private BackEndFunctionalTestScenarioContext parentContext;
    private Map<String, BackEndFunctionalTestScenarioContext> childContexts = new HashMap<>();


    public void addChildContext(BackEndFunctionalTestScenarioContext childContext) {
        childContext.setParentContext(this);
        childContexts.put(childContext.getTestData().get_guid_(), childContext);
    }

    public BackEndFunctionalTestScenarioContext getParentContext() {
        return parentContext;
    }

    public void setParentContext(BackEndFunctionalTestScenarioContext parentContext) {
        this.parentContext = parentContext;
    }

    public Map<String, BackEndFunctionalTestScenarioContext> getChildContexts() {
        return childContexts;
    }

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
