package uk.gov.hmcts.ccd.fta.steps;

import java.io.IOException;

public interface BackEndFunctionalTestAutomationDSL {

    // DSL Element:
    // "an appropriate test context as detailed in the test data source"
    void initializeAppropriateTestContextAsDetailedInTheTestDataSource();

    // DSL Element:
    // "a case has just been created as in [{}]"
    public void prepareACase(String caseSpecification);

    // DSL Element:
    // "a user with [<some specification about user data>]"
    void verifyThatThereIsAUserInTheContextWithAParticularSpecification(String specificationAboutAUser);

    // DSL Element:
    // "a request is prepared with appropriate values"
    public void prepareARequestWithAppropriateValues() throws IOException;

    // DSL Element:
    // "the response [{}]"
    public void verifyTheRequestInTheContextWithAParticularSpecification(String requestSpecification);

    // DSL Element:
    // "it is submitted to call the {} operation of {}"
    public void submitTheRequestToCallAnOperationOfAProduct(String operation, String productName) throws IOException;

    // DSL Element:
    // "a positive response is received"
    public void verifyThatAPositiveResponseWasReceived();

    // DSL Element:
    // "a negative response is received"
    public void verifyThatANegativeResponseWasReceived();

    // DSL Element:
    // "the response has all the details as expected"
    public void verifyThatTheResponseHasAllTheDetailsAsExpected() throws IOException;

    // DSL Element:
    // "the response [{}]"
    public void verifyTheResponseInTheContextWithAParticularSpecification(String responseSpecification);
}
