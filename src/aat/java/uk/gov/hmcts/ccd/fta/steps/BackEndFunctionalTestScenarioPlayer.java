package uk.gov.hmcts.ccd.fta.steps;

import feign.FeignException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;

public class BackEndFunctionalTestScenarioPlayer implements BackEndFunctionalTestAutomationDSL {

    private final BackEndFunctionalTestScenarioContext scenarioContext;
    private final AATHelper aat;
    private Scenario scenario;

    private Logger logger = LoggerFactory.getLogger(BackEndFunctionalTestScenarioPlayer.class);

    public BackEndFunctionalTestScenarioPlayer() {
        aat = AATHelper.INSTANCE;
        RestAssured.baseURI = aat.getTestUrl();
        RestAssured.useRelaxedHTTPSValidation();
        scenarioContext = new BackEndFunctionalTestScenarioContext();
    }

    @Before()
    public void prepare(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    @Given("an appropriate test context as detailed in the test data source")
    public void initializeAppropriateTestContextAsDetailedInTheTestDataSource() {
        scenarioContext.initializeTestDataFor(scenario);
        String logPrefix = scenarioContext.getCurrentScenarioTag() + ": Test data ";
        if (scenarioContext.getTestData() != null) {
            logger.info(logPrefix + "was loaded successfully");
        } else {
            logger.info(logPrefix + "was not found");
        }
    }

    @Override
    @Given("a user with [{}]")
    public void verifyThatThereIsAUserInTheContextWithAParticularSpecification(String specificationAboutAUser) {
        UserData aUser = scenarioContext.getTestData().getUser();

        String logPrefix = scenarioContext.getCurrentScenarioTag() + ": User ";
        try {
            AuthenticatedUser authenticatedUserMetadata = aat.getIdamHelper().authenticate(
                aUser.getUsername(), aUser.getPassword());
            aUser.setToken(authenticatedUserMetadata.getAccessToken());
            aUser.setUid(authenticatedUserMetadata.getId());
            logger.info(logPrefix + "authenticated");
        } catch (FeignException ex) {
            logger.info(logPrefix + "credentials do not exist");
        }

        scenarioContext.setTheUser(aUser);

        boolean doesTestDataMeetSpec = scenarioContext.getTestData().meetsSpec(specificationAboutAUser);
        String errorMessage = "Test data does not confirm it meets the specification about a user: "
            + specificationAboutAUser;
        Assert.assertTrue(errorMessage, doesTestDataMeetSpec);
    }

    @Override
    @When("a request is prepared with appropriate values")
    public void prepareARequestWithAppropriateValues() {
        UserData theUser = scenarioContext.getTheUser();
        String s2sToken = aat.getS2SHelper().getToken();

        Assert.assertNotNull("User not authenticated, request cannot be created", theUser.getUid());

        RequestSpecification aRequest = RestAssured.given()
                .header("Authorization", "Bearer " + theUser.getToken())
                .header("ServiceAuthorization", s2sToken)
                .pathParam("uid", theUser.getUid());

        scenarioContext.setTheRequest(aRequest);
    }

    @Override
    @When("it is submitted to call the {} operation of {}")
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName) {
        RequestSpecification theRequest = scenarioContext.getTheRequest();
        String uri = scenarioContext.getTestData().getUri();
        System.out.println("URI: " + RestAssured.baseURI + uri);
        Response response = theRequest.get(uri);
        scenarioContext.setTheResponse(response);
    }

    @Override
    @Then("a positive response is received")
    public void verifyThatAPositiveResponseWasReceived() {
        int responseCode = scenarioContext.getTheResponse().getStatusCode();
        String message = "Response code is not a success code. It is: " + responseCode;
        Assert.assertEquals(message, 2, responseCode / 100);
    }

    @Override
    @Then("a negative response is received")
    public void verifyThatANegativeResponseWasReceived() {
        int code = scenarioContext.getTheResponse().getStatusCode();
        String message = "Response code is not a negative one. It is: " + code;
        Assert.assertNotEquals(message, 2, code / 100);
    }

    @Override
    @Then("the response has all the details as expected")
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() {
        Response theResponse = scenarioContext.getTheResponse();
        ResponseData actualResponse = new ResponseData();
        actualResponse.setResponseCode(theResponse.getStatusCode());
        actualResponse.setBody(theResponse.getBody().toString());
        ResponseData expectedResponse = scenarioContext.getTestData().getExpectedResponse();
        List<String> validationErrors = compareResponses(actualResponse, expectedResponse);
        String message = "Actual and expected responses do not match: " + validationErrors;
        Assert.assertTrue(message, validationErrors.isEmpty());
    }

    private List<String> compareResponses(ResponseData actualResponse, ResponseData expectedResponse) {
        List<String> validationErrors = new ArrayList<>();
        if (expectedResponse.getResponseCode() != actualResponse.getResponseCode()) {
            validationErrors.add("Response code mismatch, expected: " + expectedResponse.getResponseCode()
                + ", actual: " + actualResponse.getResponseCode());
        }
        if (actualResponse.getBody().contains(expectedResponse.getBody())) {
            validationErrors.add("Response body mismatch, expected: " + expectedResponse.getBody()
                + ", actual: " + actualResponse.getBody());
        }
        return validationErrors;
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
