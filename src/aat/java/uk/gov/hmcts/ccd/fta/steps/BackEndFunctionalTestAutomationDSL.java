package uk.gov.hmcts.ccd.fta.steps;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface BackEndFunctionalTestAutomationDSL {

    // DSL Element:
    // "an appropriate test context as detailed in the test data source"
    void initializeAppropriateTestContextAsDetailedInTheTestDataSource();

    // DSL Element:
    // "a user with [<some specification about user data>]"
    void verifyThatThereIsAUserInTheContextWithAParticularSpecification(String specificationAboutAUser);

    // DSL Element:
    // "a request is prepared with appropriate values"
    public void prepareARequestWithAppropriateValues() throws JsonProcessingException;

    // DSL Element:
    // "it is submitted to call the {} operation of {}"
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName);

    // DSL Element:
    // "a positive response is received"
    public void verifyThatAPositiveResponseWasReceived();

    // DSL Element:
    // "a negative response is received"
    public void verifyThatANegativeResponseWasReceived();

    // DSL Element:
    // "the response has all the details as expected"
    public void verifyThatTheResponseHasAllTheDetailsAsExpected();

    // DSL Element:
    // "the response [{}]"
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification);
}
