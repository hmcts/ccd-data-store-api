package uk.gov.hmcts.ccd.fta.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import feign.FeignException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;
import uk.gov.hmcts.ccd.datastore.tests.fixture.CCDEventBuilder;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.RequestData;
import uk.gov.hmcts.ccd.fta.data.ResponseData;
import uk.gov.hmcts.ccd.fta.data.UserData;
import uk.gov.hmcts.ccd.fta.exception.FunctionalTestException;
import uk.gov.hmcts.ccd.fta.util.EnvUtils;
import uk.gov.hmcts.ccd.fta.util.JsonUtils;

@SuppressWarnings({ "LocalVariableName" })
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
    @Given("a case that has just been created as in [{}]")
    public void createCaseWithTheDataProvidedInATestDataObject(String caseDataId) {
        scenarioContext.initializeCaseCreationDataFor(caseDataId);
        HttpTestData caseData = scenarioContext.getCaseCreationData();

        UserData caseCreator = caseData.getInvokingUser();
        resolveUserData("caseCreator", caseCreator);
        authenticateUser("caseCreator", caseCreator);

        Supplier<RequestSpecification> asCaseCreator = () -> RestAssured.given()
                .header("Authorization", "Bearer " + caseCreator.getToken())
                .header("ServiceAuthorization", aat.getS2SHelper().getToken()).pathParam("user", caseCreator.getUid());

        Map<String, Object> caseVariables = caseData.getRequest().getPathVariables();
        String jurisdiction = caseVariables.get("jurisdiction").toString();
        String caseType = caseVariables.get("caseType").toString();
        String event = caseVariables.get("event").toString();
        AATCaseType.CaseData data;
        try {
            data = new ObjectMapper().convertValue(caseData.getRequest().getBody(), AATCaseType.CaseData.class);
        } catch (IllegalArgumentException ex) {
            String errorMessage = "Cannot map '" + caseDataId + "' -> request.body to AATCaseType.CaseData object";
            throw new FunctionalTestException(errorMessage);
        }

        String eventToken = aat.getCcdHelper().generateTokenCreateCase(asCaseCreator, jurisdiction, caseType, event);
        Long caseReference = new CCDEventBuilder(jurisdiction, caseType, event).as(asCaseCreator).withData(data)
                .withEventId(event).withToken(eventToken).submitAndGetReference();

        scenarioContext.setTheCaseReference(caseReference);
        scenario.write("Created a case with reference: " + caseReference);
    }

    @Override
    @Given("a user with [{}]")
    public void verifyThatThereIsAUserInTheContextWithAParticularSpecification(String specificationAboutAUser) {
        UserData aUser = scenarioContext.getTestData().getInvokingUser();
        resolveUserData("users.invokingUser", aUser);
        scenario.write("Invoking user: " + aUser.getUsername());
        authenticateUser("users.invokingUser", aUser);
        scenarioContext.setTheInvokingUser(aUser);

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
        prepareARequestWithAppropriateValues(this.scenarioContext);
    }

    private void prepareARequestWithAppropriateValues(BackEndFunctionalTestScenarioContext scenarioContext)
            throws IOException {
        if (scenarioContext.getTheInvokingUser() == null) {
            UserData anInvokingUser = scenarioContext.getTestData().getInvokingUser();
            resolveUserData("users.invokingUser", anInvokingUser);
            authenticateUser("users.invokingUser", anInvokingUser);
            scenarioContext.setTheInvokingUser(anInvokingUser);
        }

        HttpTestData testData = scenarioContext.getTestData();

        new DynamicValueInjector(aat, testData, scenarioContext).injectDataFromContext();

        RequestSpecification raRequest = buildRestAssuredRequestWith(testData.getRequest());

        scenarioContext.setTheRequest(raRequest);
        scenario.write("Request prepared with the following variables: "
                + JsonUtils.getPrettyJsonFromObject(scenarioContext.getTestData().getRequest()));
    }


    private RequestSpecification buildRestAssuredRequestWith(RequestData requestData) throws IOException {
        RequestSpecification aRequest = RestAssured.given();

        if (requestData.getHeaders() != null) {
            requestData.getHeaders().forEach((header, value) -> aRequest.header(header, value));
        }

        if (requestData.getPathVariables() != null) {
            requestData.getPathVariables().forEach((pathVariable, value) -> aRequest.pathParam(pathVariable, value));
        }

        if (requestData.getQueryParams() != null) {
            requestData.getQueryParams().forEach((queryParam, value) -> aRequest.queryParam(queryParam, value));
        }

        if (requestData.getBody() != null) {
            aRequest.body(new ObjectMapper().writeValueAsBytes(requestData.getBody()));
        }
        return aRequest;
    }

    @Override
    @When("the request [{}]")
    public void verifyTheRequestInTheContextWithAParticularSpecification(String requestSpecification) {
        verifyTheRequestInTheContextWithAParticularSpecification(this.scenarioContext, requestSpecification);
    }

    private void verifyTheRequestInTheContextWithAParticularSpecification(
            BackEndFunctionalTestScenarioContext scenarioContext, String requestSpecification) {
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
        submitTheRequestToCallAnOperationOfAProduct(this.scenarioContext, operation, productName);
    }

    @SuppressWarnings("unchecked")
    private void submitTheRequestToCallAnOperationOfAProduct(BackEndFunctionalTestScenarioContext scenarioContext,
            String operation, String productName) throws IOException {
        boolean isCorrectOperation = scenarioContext.getTestData().meetsOperationOfProduct(operation, productName);
        if (!isCorrectOperation) {
            String errorMessage = "Test data does not confirm it is calling the following operation of a product: "
                    + operation + " -> " + productName;
            throw new FunctionalTestException(errorMessage);
        }

        RequestSpecification theRequest = scenarioContext.getTheRequest();
        String uri = scenarioContext.getTestData().getUri();
        String methodAsString = scenarioContext.getTestData().getMethod();
        Method method;
        try {
            method = Method.valueOf(methodAsString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            String errorMessage = "Method '" + methodAsString + "' in test data file not recognised";
            throw new FunctionalTestException(errorMessage);
        }

        Response response = theRequest.request(method, uri);
        QueryableRequestSpecification queryableRequest = SpecificationQuerier.query(theRequest);
        scenario.write("Calling " + queryableRequest.getMethod() + " " + queryableRequest.getURI());

        Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        response.getHeaders().forEach(header -> responseHeaders.put(header.getName(), header.getValue()));
        ResponseData responseData = new ResponseData();
        responseData.setResponseCode(response.getStatusCode());
        responseData.setResponseMessage(HttpStatus.valueOf(response.getStatusCode()).getReasonPhrase());
        responseData.setHeaders(responseHeaders);

        if (!response.getBody().asString().isEmpty()) {
            String apiResponse = convertArrayJsonToMapJson(response.getBody().asString());
            responseData.setBody(JsonUtils.readObjectFromJsonText(apiResponse, Map.class));
        }
        scenarioContext.getTestData().setActualResponse(responseData);
        scenarioContext.setTheResponse(responseData);
    }

    private String convertArrayJsonToMapJson(String apiResponse) {
        if (apiResponse.startsWith("[") && apiResponse.endsWith("]")) {
            apiResponse = "{\"arrayInMap\":" + apiResponse + "}";
        }
        return apiResponse;
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
    @Then("the response has all other details as expected")
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() throws IOException {
        verifyThatTheResponseHasAllTheDetailsAsExpected(this.scenarioContext);
    }

    private void verifyThatTheResponseHasAllTheDetailsAsExpected(BackEndFunctionalTestScenarioContext scenarioContext)
            throws IOException {
        ResponseData expectedResponse = scenarioContext.getTestData().getExpectedResponse();
        ResponseData actualResponse = scenarioContext.getTheResponse();
        Map<String, List<?>> issues = new HashMap<>();

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

    @Override
    @Given("a successful call [{}] as in [{}]")
    @Given("another successful call [{}] as in [{}]")
    @Then("a call [{}] will get the expected response as in [{}]")
    @Then("another call [{}] will get the expected response as in [{}]")
    public void performAndVerifyTheExpectedResponseForAnApiCall(String testDataSpec, String testDataId)
            throws IOException {
        BackEndFunctionalTestScenarioContext subcontext = new BackEndFunctionalTestScenarioContext();
        subcontext.initializeTestDataFor(testDataId);
        this.scenarioContext.addChildContext(subcontext);
        prepareARequestWithAppropriateValues(subcontext);
        verifyTheRequestInTheContextWithAParticularSpecification(subcontext, testDataSpec);
        submitTheRequestToCallAnOperationOfAProduct(subcontext, subcontext.getTestData().getOperationName(),
                subcontext.getTestData().getProductName());
        verifyThatTheResponseHasAllTheDetailsAsExpected(subcontext);
    }

    private void resolveUserData(String prefix, UserData aUser) {
        String resolvedUsername = EnvUtils.resolvePossibleEnvironmentVariable(aUser.getUsername());
        if (resolvedUsername.equals(aUser.getUsername())) {
            logger.info(scenarioContext.getCurrentScenarioTag() + ": Expected environment variable declaration "
                    + "for " + prefix + ".username but found '" + resolvedUsername + "', which may cause issues "
                    + "in higher environments");
        }

        String resolvedPassword = EnvUtils.resolvePossibleEnvironmentVariable(aUser.getPassword());
        if (resolvedPassword.equals(aUser.getPassword())) {
            logger.info(scenarioContext.getCurrentScenarioTag() + ": Expected environment variable declaration "
                    + "for " + prefix + ".password but found '" + resolvedPassword + "', which may cause issues "
                    + "in higher environments");
        }

        aUser.setUsername(resolvedUsername);
        aUser.setPassword(resolvedPassword);
    }

    private void authenticateUser(String prefix, UserData aUser) {
        String logPrefix = scenarioContext.getCurrentScenarioTag() + ": " + prefix + " [" + aUser.getUsername() + "]["
                + aUser.getPassword() + "] ";
        try {
            AuthenticatedUser authenticatedUserMetadata = aat.getIdamHelper().authenticate(aUser.getUsername(),
                    aUser.getPassword());
            aUser.setToken(authenticatedUserMetadata.getAccessToken());
            aUser.setUid(authenticatedUserMetadata.getId());
            logger.info(logPrefix + "authenticated");
        } catch (FeignException ex) {
            logger.info(logPrefix + "credentials invalid");
        }
    }
}
