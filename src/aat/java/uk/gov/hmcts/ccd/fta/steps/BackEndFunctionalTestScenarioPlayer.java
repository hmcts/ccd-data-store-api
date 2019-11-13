package uk.gov.hmcts.ccd.fta.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.junit.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.fta.data.RequestData;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;
import uk.gov.hmcts.ccd.fta.util.JsonUtils;

public class BackEndFunctionalTestScenarioPlayer implements BackEndFunctionalTestAutomationDSL {

    private static final String DYNAMIC_CONTENT_PLACEHOLDER = "[[DYNAMIC]]";

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
    public void prepareARequestWithAppropriateValues() throws IOException {
        UserData theUser = scenarioContext.getTheUser();
        String s2sToken = aat.getS2SHelper().getToken();

        RequestSpecification aRequest = RestAssured.given();
        RequestData requestData = scenarioContext.getTestData().getRequest();

        if (requestData.getHeaders() != null) {
            requestData.getHeaders().forEach((header, value) -> {
                if (value.toString().equals(DYNAMIC_CONTENT_PLACEHOLDER)) {
                    // ADD DYNAMIC DATA HERE
                    if (header.equals("Authorization")) {
                        String authToken = "Bearer " + theUser.getToken();
                        aRequest.header(header, authToken);
                        scenarioContext.getTestData().getRequest().getHeaders().put("Authorization", authToken);
                    } else if (header.equals("ServiceAuthorization")) {
                        aRequest.header(header, s2sToken);
                        scenarioContext.getTestData().getRequest().getHeaders().put("ServiceAuthorization", s2sToken);
                    }
                } else {
                    aRequest.header(header, value);
                }
            });
        }

        if (requestData.getPathVariables() != null) {
            requestData.getPathVariables().forEach((pathVariable, value) -> {
                if (value.toString().equals(DYNAMIC_CONTENT_PLACEHOLDER)) {
                    // ADD DYNAMIC DATA HERE
                    if (pathVariable.equals("uid")) {
                        aRequest.pathParam(pathVariable, theUser.getUid());
                        scenarioContext.getTestData().getRequest().getPathVariables().put("uid", theUser.getUid());
                    }
                } else {
                    aRequest.pathParam(pathVariable, value);
                }
            });
        }

        if (requestData.getQueryParams() != null) {
            requestData.getQueryParams().forEach((queryParam, value) -> {
                if (value.toString().equals(DYNAMIC_CONTENT_PLACEHOLDER)) {
                    // ADD DYNAMIC DATA HERE
                } else {
                    aRequest.queryParam(queryParam, value);
                }
            });
        }

        if (requestData.getBody() != null) {
            aRequest.body(new ObjectMapper().writeValueAsBytes(requestData.getBody()));
        }

        scenarioContext.setTheRequest(aRequest);
        scenario.write(JsonUtils.getPrettyJsonFromObject(scenarioContext.getTestData().getRequest()));
    }

    @Override
    @When("it is submitted to call the [{}] operation of [{}]")
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName) throws IOException {
        boolean isCorrectOperation = scenarioContext.getTestData().meetsOperationOfProduct(operation, productName);
        String errorMessage = "Test data does not confirm it is calling the following operation of a product: "
            + operation + " -> " + productName;
        Assert.assertTrue(errorMessage, isCorrectOperation);

        RequestSpecification theRequest = scenarioContext.getTheRequest();
        String uri = scenarioContext.getTestData().getUri();

        Response response = null;
        switch (scenarioContext.getTestData().getMethod()) {
            case "GET":
                response = theRequest.get(uri);
                break;
            case "POST":
                response = theRequest.post(uri);
                break;
            case "PUT":
                response = theRequest.put(uri);
                break;
            case "DELETE":
                response = theRequest.delete(uri);
                break;
            default:
                Assert.fail("Unknown request method in data file");
        }

        QueryableRequestSpecification queryableRequest = SpecificationQuerier.query(theRequest);
        scenario.write(queryableRequest.getMethod() + " " + queryableRequest.getURI());

        Map<String, Object> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(header -> responseHeaders.put(header.getName(), header.getValue()));
        ResponseData responseData = new ResponseData();
        responseData.setResponseCode(response.getStatusCode());
        responseData.setResponseMessage(HttpStatus.valueOf(response.getStatusCode()).getReasonPhrase());
        responseData.setHeaders(responseHeaders);
        responseData.setBody(JsonUtils.readObjectFromJsonText(response.getBody().asString(), Map.class));

        scenarioContext.setTheResponse(responseData);
    }

    @Override
    @Then("a positive response is received")
    public void verifyThatAPositiveResponseWasReceived() {
        int responseCode = scenarioContext.getTheResponse().getResponseCode();
        String errorMessage = "Response code is not a success code. It is: " + responseCode;
        Assert.assertEquals(errorMessage, 2, responseCode / 100);
        scenario.write("" + scenarioContext.getTheResponse().getResponseCode());
    }

    @Override
    @Then("a negative response is received")
    public void verifyThatANegativeResponseWasReceived() {
        int code = scenarioContext.getTheResponse().getResponseCode();
        String errorMessage = "Response code is not a negative one. It is: " + code;
        Assert.assertNotEquals(errorMessage, 2, code / 100);
        scenario.write("" + scenarioContext.getTheResponse().getResponseCode());
    }

    @Override
    @Then("the response has all the details as expected")
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() throws IOException {
        // TODO: write response comparison logic
        Map<String, Object> expectedResponseBody = scenarioContext.getTestData().getExpectedResponse().getBody();
        Map<String, Object> actualResponseBody = scenarioContext.getTheResponse().getBody();
        MapVerificationResult mapVerificationResult = MapVerifier.verifyMap(expectedResponseBody, actualResponseBody, 10);
        logger.info("Response body issues: " + mapVerificationResult.getAllIssues().toString());

        scenario.write(JsonUtils.getPrettyJsonFromObject(scenarioContext.getTheResponse()));
    }

    @Override
    @Then("the response [{}]")
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification) {
        String errorMessage = "Test data does not confirm it meets the specification about the response: "
                + responseSpecification;
        boolean check = scenarioContext.getTestData().meetsSpec(responseSpecification);
        Assert.assertTrue(errorMessage, check);
    }
}
