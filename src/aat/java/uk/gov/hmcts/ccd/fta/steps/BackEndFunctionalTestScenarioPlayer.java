package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

import java.util.List;

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

    @Given("an appropriate test context as detailed in the test data source,")
    public void an_appropriate_test_context_as_detailed_in_the_test_data_source() {

    }

    @Given("a user with an existing case in CCD,")
    public void a_user_with_an_existing_case_in_CCD() {

    }

    @When("a request is prepared with appropriate values,")
    public void a_request_is_prepared_with_appropriate_values() {

    }

    @When("failed to provide any authentication credentials within the request.")
    public void failed_to_provide_any_authentication_credentials_within_the_request() {

    }

    @Then("a negative response is received,")
    public void a_negative_response_is_received() {

    }

    @Then("the response has all the details as expected.")
    public void the_response_has_all_the_details_as_expected() {

    }

    @When("a user Access Denied i.e. you don't have permission to access,")
    public void a_user_Access_Denied_i_e_you_don_t_have_permission_to_access() {

    }

    @Given("a user with a non-existing Idam user ID in CCD,")
    public void a_user_with_a_non_existing_Idam_user_ID_in_CCD() {

    }

    @When("the request body can't be parsed")
    public void the_request_body_can_t_be_parsed() {

    }

    @Then("a positive response is received,")
    public void a_positive_response_is_received() {

    }

    @Given("a user with a non-existing Jurisdiction ID in CCD,")
    public void a_user_with_a_non_existing_Jurisdiction_ID_in_CCD() {

    }

    @Given("a user with a non-existing Case type ID in CCD,")
    public void a_user_with_a_non_existing_Case_type_ID_in_CCD() {

    }

    @Given("a user with a non-existing Case ID in CCD,")
    public void a_user_with_a_non_existing_Case_ID_in_CCD() {

    }

    @Given("a user with a non-existing Event ID in CCD,")
    public void a_user_with_a_non_existing_Event_ID_in_CCD() {

    }


}
