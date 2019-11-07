package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;

import java.util.List;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

public class BackEndFunctionalTestScenarioPlayer implements BackEndFunctionalTestAtuomationDSL {

    private final BackEndFunctionalTestScenarioContext scenarioContext;

    private Scenario scenario;

    public BackEndFunctionalTestScenarioPlayer() {
        scenarioContext = new BackEndFunctionalTestScenarioContext();
    }

    @Before()
    public void prepare(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    @Given("an appropriate test context as detailed in the test data source")
    public void initializeAppropriateTestContextAsDetailedInTheTestDataSource() {
        scenarioContext.initilizeTestDataFor(scenario);
        // log that the context has been initialised
    }

    @Override
    @Given("a user with [{}]")
    public void verifyThatThereIsAUserInTheContextWithAParticularSpecification(String specificationAboutAUser) {
        UserData aUser = scenarioContext.getTestData().getUser();
        scenarioContext.setAUser(aUser);
        String userToken = acquireTokenFor(aUser);
        scenarioContext.getAUser().setToken(userToken);
        String message = "Test data does not confirm it meets the specification about a user: "
                + specificationAboutAUser;
        boolean check = scenarioContext.getTestData().meetsSpec(specificationAboutAUser);
        Assert.assertTrue(message, check);
    }

    private String acquireTokenFor(UserData aUser) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @When("a request is prepared with appropriate values")
    public void prepareARequestWithAppropriateValues() {
        Object aRequest = null;
        // TODO: Prepare a request
        scenarioContext.setTheRequest(aRequest);
    }

    @Override
    @When("it is submitted to call the {} operation of {}")
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName) {
        Object theRequest = scenarioContext.getTheRequest();
        // TODO: use RestAssured to place the call
        ResponseData response = new ResponseData();
        response.setResponseCode(201);
        // TODO: Set this from rest assured call
        scenarioContext.setTheResponse(response);
    }

    @Override
    @Then("a positive response is received")
    public void verifyThatAPositiveResponseWasReceived() {
        int code = scenarioContext.getTheResponse().getResponseCode();
        String message = "Response code is not a success code. It is: " + code;
        Assert.assertEquals(message, 2, code / 100);
    }

    @Override
    @Then("a negative response is received")
    public void verifyThatANegativeResponseWasReceived() {
        int code = scenarioContext.getTheResponse().getResponseCode();
        String message = "Response code is not a negative one. It is: " + code;
        Assert.assertNotEquals(message, 2, code / 100);
    }

    @Override
    @Then("the response has all the details as expected")
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() {
        ResponseData actualResponse = scenarioContext.getTheResponse();
        ResponseData expectedResponse = scenarioContext.getTestData().getExpectedResponse();
        List<String> issues = compareResponses(actualResponse, expectedResponse);
        String message = "Actual and expected responses do not match: " + issues;
        Assert.assertTrue(message, issues == null);
    }

    private List<String> compareResponses(ResponseData actualResponse, ResponseData expectedResponse) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Then("the response [{}]")
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification) {
        String message = "Test data does not confirm it meets the specification about the response: "
                + responseSpecification;
        boolean check = scenarioContext.getTestData().meetsSpec(responseSpecification);
        Assert.assertTrue(message, check);
    }

}
