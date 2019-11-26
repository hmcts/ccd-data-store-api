package uk.gov.hmcts.ccd.fta.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.fta.data.RequestData;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;
import uk.gov.hmcts.ccd.fta.exception.FunctionalTestException;
import uk.gov.hmcts.ccd.fta.util.EnvUtils;
import uk.gov.hmcts.ccd.fta.util.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"LocalVariableName"})
public class BackEndFunctionalTestScenarioPlayer implements BackEndFunctionalTestAutomationDSL {

    private static final String DYNAMIC_CONTENT_PLACEHOLDER = "[[DYNAMIC]]";
    private static boolean isTestDataLoaded = false;

    private final String BE_FTA_FILE_JURISDICTION1 = "src/aat/resources/CCD_BE_FTA_JURISDICTION1.xlsx";
    private final String BE_FTA_FILE_JURISDICTION2 = "src/aat/resources/CCD_BE_FTA_JURISDICTION2.xlsx";
    private final String BE_FTA_FILE_JURISDICTION3 = "src/aat/resources/CCD_BE_FTA_JURISDICTION3.xlsx";

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
        if (!isTestDataLoaded) {
            importDefinitions();
        }
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

        String resolvedUsername = EnvUtils.resolvePossibleEnvironmentVariable(aUser.getUsername());
        if (resolvedUsername.equals(aUser.getUsername())) {
            logger.info(scenarioContext.getCurrentScenarioTag() + ": Expected environment variable declaration "
                + "for user.username but found '" + resolvedUsername + "', which may cause issues in higher "
                + "environments");
        }

        String resolvedPassword = EnvUtils.resolvePossibleEnvironmentVariable(aUser.getPassword());
        if (resolvedPassword.equals(aUser.getPassword())) {
            logger.info(scenarioContext.getCurrentScenarioTag() + ": Expected environment variable declaration "
                + "for user.password but found '" + resolvedPassword + "', which may cause issues in higher "
                + "environments");
        }

        aUser.setUsername(resolvedUsername);
        aUser.setPassword(resolvedPassword);
        scenario.write("User: " + resolvedUsername);

        String logPrefix = scenarioContext.getCurrentScenarioTag() + ": Idam user [" + aUser.getUsername()
            + "][" + aUser.getPassword() + "] ";
        try {
            AuthenticatedUser authenticatedUserMetadata = aat.getIdamHelper().authenticate(
                aUser.getUsername(), aUser.getPassword());
            aUser.setToken(authenticatedUserMetadata.getAccessToken());
            aUser.setUid(authenticatedUserMetadata.getId());
            logger.info(logPrefix + "authenticated");
        } catch (FeignException ex) {
            logger.info(logPrefix + "credentials invalid");
        }

        scenarioContext.setTheUser(aUser);

        boolean doesTestDataMeetSpec = scenarioContext.getTestData().meetsSpec(specificationAboutAUser);
        if (!doesTestDataMeetSpec) {
            String errorMessage = "Test data does not confirm it meets the specification about a user: "
                + specificationAboutAUser;
            throw new FunctionalTestException(errorMessage);
        }
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
                    if (header.equals("Authorization") && theUser.getToken() != null) {
                        String authToken = "Bearer " + theUser.getToken();
                        aRequest.header(header, authToken);
                        scenarioContext.getTestData().getRequest().getHeaders().put("Authorization", authToken);
                    } else if (header.equals("ServiceAuthorization") && s2sToken != null) {
                        aRequest.header(header, s2sToken);
                        scenarioContext.getTestData().getRequest().getHeaders().put("ServiceAuthorization", s2sToken);
                    } else {
                        throw new FunctionalTestException("Dynamic value for request header '" + header
                            + "' does not exist");
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
                    if (pathVariable.equals("uid") && theUser.getUid() != null) {
                        aRequest.pathParam(pathVariable, theUser.getUid());
                        scenarioContext.getTestData().getRequest().getPathVariables().put("uid", theUser.getUid());
                    } else {
                        throw new FunctionalTestException("Dynamic value for request path variable '"
                            + pathVariable + "' does not exist");
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
                    throw new FunctionalTestException("Dynamic value for request query parameter '"
                        + queryParam + "' does not exist");
                } else {
                    aRequest.queryParam(queryParam, value);
                }
            });
        }

        if (requestData.getBody() != null) {
            aRequest.body(new ObjectMapper().writeValueAsBytes(requestData.getBody()));
        }

        scenarioContext.setTheRequest(aRequest);
        scenario.write("Request prepared with the following variables: "
            + JsonUtils.getPrettyJsonFromObject(scenarioContext.getTestData().getRequest()));
    }

    @Override
    @When("the request [{}]")
    public void verifyTheRequestInTheContextWithAParticularSpecification(String requestSpecification) {
        boolean check = scenarioContext.getTestData().meetsSpec(requestSpecification);
        if (!check) {
            String errorMessage = "Test data does not confirm it meets the specification about the request: "
                + requestSpecification;
            throw new FunctionalTestException(errorMessage);
        }
    }

    @Override
    @When("it is submitted to call the [{}] operation of [{}]")
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName) throws IOException {
        boolean isCorrectOperation = scenarioContext.getTestData().meetsOperationOfProduct(operation, productName);
        if (!isCorrectOperation) {
            String errorMessage = "Test data does not confirm it is calling the following operation of a product: "
                + operation + " -> " + productName;
            throw new FunctionalTestException(errorMessage);
        }

        RequestSpecification theRequest = scenarioContext.getTheRequest();
        String uri = scenarioContext.getTestData().getUri();

        String method = scenarioContext.getTestData().getMethod();
        if (method == null) {
            throw new FunctionalTestException("No request method in data file");
        }

        Response response;
        switch (method) {
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
                throw new FunctionalTestException("Unknown request method in data file");
        }

        QueryableRequestSpecification queryableRequest = SpecificationQuerier.query(theRequest);
        scenario.write("Calling " + queryableRequest.getMethod() + " " + queryableRequest.getURI());

        Map<String, Object> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(header -> responseHeaders.put(header.getName(), header.getValue()));
        ResponseData responseData = new ResponseData();
        responseData.setResponseCode(response.getStatusCode());
        responseData.setResponseMessage(HttpStatus.valueOf(response.getStatusCode()).getReasonPhrase());
        responseData.setHeaders(responseHeaders);

        if (!response.getBody().asString().isEmpty()) {
            responseData.setBody(JsonUtils.readObjectFromJsonText(response.getBody().asString(), Map.class));
        }

        scenarioContext.setTheResponse(responseData);
    }

    @Override
    @Then("a positive response is received")
    public void verifyThatAPositiveResponseWasReceived() {
        int responseCode = scenarioContext.getTheResponse().getResponseCode();
        scenario.write("Response code: " + responseCode);
        if (responseCode / 100 != 2) {
            String errorMessage = "Response code '" + responseCode + "' is not a success code";
            throw new FunctionalTestException(errorMessage);
        }
    }

    @Override
    @Then("a negative response is received")
    public void verifyThatANegativeResponseWasReceived() {
        int responseCode = scenarioContext.getTheResponse().getResponseCode();
        scenario.write("Response code: " + responseCode);
        if (responseCode / 100 == 2) {
            String errorMessage = "Response code '" + responseCode + "' is a success code";
            throw new FunctionalTestException(errorMessage);
        }
    }

    @Override
    @Then("the response has all the details as expected")
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() throws IOException {
        ResponseData expectedResponse = scenarioContext.getTestData().getExpectedResponse();
        ResponseData actualResponse = scenarioContext.getTheResponse();
        Map<String, List> issues = new HashMap<>();

        if (actualResponse.getResponseCode() != expectedResponse.getResponseCode()) {
            issues.put("responseCode", Collections.singletonList("Response code mismatch, expected: "
                + expectedResponse.getResponseCode() + ", actual: " + actualResponse.getResponseCode()));
        }

        MapVerificationResult headerVerification = MapVerifier.verifyMap("actualResponse.headers",
            expectedResponse.getHeaders(), actualResponse.getHeaders(), 1);
        if (!headerVerification.isVerified()) {
            issues.put("headers", headerVerification.getAllIssues());
        }

        MapVerificationResult bodyVerification = MapVerifier.verifyMap("actualResponse.body",
            expectedResponse.getBody(), actualResponse.getBody(), 20);
        if (!bodyVerification.isVerified()) {
            issues.put("body", bodyVerification.getAllIssues());
        }

        scenario.write("Response: " + JsonUtils.getPrettyJsonFromObject(scenarioContext.getTheResponse()));

        if (issues.get("responseCode") != null || issues.get("headers") != null || issues.get("body") != null) {
            String errorMessage = "Response failures: " + JsonUtils.getPrettyJsonFromObject(issues);
            throw new FunctionalTestException(errorMessage);
        }
    }

    @Override
    @Then("the response [{}]")
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification) {
        boolean check = scenarioContext.getTestData().meetsSpec(responseSpecification);
        if (!check) {
            String errorMessage = "Test data does not confirm it meets the specification about the response: "
                + responseSpecification;
            throw new FunctionalTestException(errorMessage);
        }
    }

    protected RequestSpecification asAutoTestImporter() {
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(aat.getImporterAutoTestEmail(),
            aat.getImporterAutoTestPassword());

        String s2sToken = aat.getS2SHelper().getToken();

        return RestAssured.given(new RequestSpecBuilder()
            .setBaseUri(aat.getDefinitionStoreUrl())
            .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }

    protected void importDefinitions() {
        importDefinition(BE_FTA_FILE_JURISDICTION1);
        importDefinition(BE_FTA_FILE_JURISDICTION2);
        importDefinition(BE_FTA_FILE_JURISDICTION3);
    }

    private void importDefinition(String file) {
        asAutoTestImporter()
            .given()
            .multiPart(new File(file))
            .expect()
            .statusCode(201)
            .when()
            .post("/import");
    }

}
