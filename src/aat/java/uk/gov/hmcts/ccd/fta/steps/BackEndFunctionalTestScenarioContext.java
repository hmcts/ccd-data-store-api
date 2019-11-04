package uk.gov.hmcts.ccd.fta.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class BackEndFunctionalTestScenarioContext {

    @Given("an appropriate test context as detailed in the test data source")
    public void anAppropriateTestContextAsDetailedInTheTestDataSource() {

    }

    @Given("a user with {} profile in CCD")
    public void aUserWithProfileInCCD(String profileType) {

    }

    @Given("they have a/an {} role")
    public void theyHaveARole(String roleName) {

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
