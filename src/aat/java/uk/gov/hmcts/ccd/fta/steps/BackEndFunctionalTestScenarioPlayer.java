package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.core.api.Scenario;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BackEndFunctionalTestScenarioPlayer {

    private final BackEndFunctionalTestScenarioContext scenarioContext;
    private HttpTestData testData;

    public BackEndFunctionalTestScenarioPlayer() {
        scenarioContext = new BackEndFunctionalTestScenarioContext();
    }

    @Given("an appropriate test context as detailed in the test data source")
    public void anAppropriateTestContextAsDetailedInTheTestDataSource() {
        Scenario scenario = BackEndFunctionalTestScenarioHooks.getScenario();
        String scenarioTag = scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@S-"))
            .collect(Collectors.joining())
            .substring(1);
        testData = scenarioContext.loadTestData(scenarioTag);
        assertThat(testData.get_guid_()).isEqualTo(scenarioTag);
    }

    @Given("a user with {} profile in CCD")
    public void aUserWithProfileInCCD(String profileType) {

    }

    @When("a request is prepared with appropriate values")
    public void aRequestIsPreparedWithAppropriateValues() {

    }

    @When("it is submitted to call the {} operation of {}")
    public void itIsSubmittedToCallTheOperationOf(String operation, String productName) {

    }

    @Then("a positive response is received")
    public void aPositiveResponseIsReceived() {

    }

    @Then("a negative response is received")
    public void aNegativeResponseIsReceived() {

    }

    @Then("the response {}")
    public void theResponse(String responseAssertion) {

    }
}
