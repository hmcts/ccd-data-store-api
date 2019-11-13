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
                        scenarioContext.getTestData().getRequest().getHeaders().put(
                            "Authorization", authToken.substring(0, 20));
                    } else if (header.equals("ServiceAuthorization")) {
                        aRequest.header(header, s2sToken);
                        scenarioContext.getTestData().getRequest().getHeaders().put(
                            "ServiceAuthorization", s2sToken.substring(0, 20));
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
        Response response = theRequest.get(uri);

        Map<String, Object> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(header -> responseHeaders.put(header.getName(), header.getValue()));

        ResponseData responseData = new ResponseData();
        responseData.setResponseCode(response.getStatusCode());
        responseData.setHeaders(responseHeaders);
        responseData.setBody(JsonUtils.readObjectFromJsonText(response.getBody().asString(), Map.class));
        scenarioContext.setTheResponse(responseData);

        QueryableRequestSpecification queryableRequest = SpecificationQuerier.query(theRequest);
        scenario.write(queryableRequest.getMethod() + " " + queryableRequest.getURI());
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
        /*Response actualResponse = scenarioContext.getTheResponse();
        ResponseData expectedResponse = scenarioContext.getTestData().getExpectedResponse();
        List<String> validationErrors = compareResponses(actualResponse, expectedResponse);
        String errorMessage = "Actual and expected responses do not match: " + validationErrors;
        Assert.assertTrue(errorMessage, validationErrors.isEmpty());*/
        // TODO: write response comparison logic
        Map<String, Object> expectedResponseBody = scenarioContext.getTestData().getExpectedResponse().getBody();
        Map<String, Object> actualResponseBody = scenarioContext.getTheResponse().getBody();
        MapVerificationResult mapVerificationResult = MapVerifier.verifyMap(expectedResponseBody, actualResponseBody, 10);
        System.out.println(mapVerificationResult.getAllIssues());

        scenario.write(JsonUtils.getPrettyJsonFromObject(scenarioContext.getTheResponse()));
    }

    /*private List<String> compareResponses(Response actualResponse, ResponseData expectedResponse) {
        List<String> validationErrors = new ArrayList<>();

        if (expectedResponse.getResponseCode() != actualResponse.getStatusCode()) {
            validationErrors.add("Response code mismatch, expected: " + expectedResponse.getResponseCode()
                + ", actual: " + actualResponse.getStatusCode());
        }

        if (expectedResponse.getHeaders() != null) {
            expectedResponse.getHeaders().forEach((expectedHeader, expectedValue) -> {
                if (!actualResponse.getHeader(expectedHeader).equals(expectedValue)) {
                    validationErrors.add("Response header mismatch, expected: " + expectedHeader + "="
                        + expectedValue + ", actual: " + expectedHeader + "="
                        + actualResponse.getHeader(expectedHeader));
                }
            });
        }

        if (expectedResponse.getBody() != null) {
            compareResponseBodyItems(actualResponse.getBody().jsonPath(), expectedResponse.getBody(),
                "", validationErrors);
        }

        return validationErrors;
    }

    private void compareResponseBodyItems(JsonPath actualResponseBody,
                                         Map<String, Object> expectedResponseBody,
                                         String expectedResponseBodyPrefix,
                                         List<String> validationErrors) {

        expectedResponseBody.forEach((expectedResponseBodyKey, expectedResponseBodyValue) -> {
            String currentPath = expectedResponseBodyPrefix + expectedResponseBodyKey;
            Object actualResponseBodyValue = actualResponseBody.get(currentPath);

            if (expectedResponseBodyValue instanceof Map) {
                compareResponseBodyItems(actualResponseBody, (Map)expectedResponseBodyValue, currentPath + ".",
                    validationErrors);
            }

            else if (expectedResponseBodyValue instanceof List) {
                List<?> expectedList = (List)expectedResponseBodyValue;
                String actualListAsString = actualResponseBodyValue.toString();
                expectedList.forEach(expectedListItem -> {
                    if (!actualListAsString.contains(expectedListItem.toString())) {
                        validationErrors.add("Response body item mismatch, expected " + currentPath + "="
                            + actualListAsString + " to contain " + expectedListItem.toString() + " but does not");
                    }
                });
            }

            else if (expectedResponseBodyValue instanceof String) {
                if (!actualResponseBodyValue.toString().equals(expectedResponseBodyValue.toString())) {
                    validationErrors.add("Response body item mismatch, expected: " + currentPath + "="
                        + expectedResponseBodyValue.toString() + ", actual: " + currentPath + "="
                        + actualResponseBodyValue.toString());
                }
            }

            else if (expectedResponseBodyValue instanceof Integer
                || expectedResponseBodyValue instanceof Double
                || expectedResponseBodyValue instanceof Date
                || expectedResponseBodyValue == null) {
                if (actualResponseBodyValue != expectedResponseBodyValue) {
                    validationErrors.add("Response body item mismatch, expected: " + currentPath + "="
                        + expectedResponseBodyValue + ", actual: " + currentPath + "=" + actualResponseBodyValue);
                }
            }

            else {
                validationErrors.add("Response body item error, unknown type at: " + currentPath);
            }
        });

    }*/

    @Override
    @Then("the response [{}]")
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification) {
        String errorMessage = "Test data does not confirm it meets the specification about the response: "
                + responseSpecification;
        boolean check = scenarioContext.getTestData().meetsSpec(responseSpecification);
        Assert.assertTrue(errorMessage, check);
    }
}
