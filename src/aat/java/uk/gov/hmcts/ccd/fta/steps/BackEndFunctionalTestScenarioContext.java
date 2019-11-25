package uk.gov.hmcts.ccd.fta.steps;

import java.util.stream.Collectors;

import io.cucumber.java.Scenario;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.fta.data.CaseData;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features", "cases" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    private Scenario scenario;
    private HttpTestData testData;
    private CaseData caseData;
    private Long theCaseReference;
    private UserData theUser;
    private RequestSpecification theRequest;
    private ResponseData theResponse;

    public void initializeTestDataFor(Scenario scenario) {
        this.scenario = scenario;
        String scenarioTag = getCurrentScenarioTag();
        testData = DATA_SOURCE.getDataForScenario(scenarioTag);
    }

    public void initializeCaseFor(String scenarioKey) {
        caseData = DATA_SOURCE.getCaseForScenario(scenarioKey);
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

    public CaseData getCaseData() {
        return caseData;
    }

    public Long getTheCaseReference() {
        return theCaseReference;
    }

    public void setTheCaseReference(Long theCaseReference) {
        this.theCaseReference = theCaseReference;
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
