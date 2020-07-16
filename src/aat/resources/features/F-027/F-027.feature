@F-027
Feature: F-027: Fetch a case for display for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-038 @Ignore # unpredictable response ordering causes test to fail
  Scenario: must return status 200 along with the case-view object successfully
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the details of the case just created]
    And the response has all other details as expected

  @S-035
  Scenario: must return appropriate negative response when case id is structurally valid but not exist in CCD
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains in input parameters a structurally valid but non-existing case-reference]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all other details as expected

  @S-036 @Ignore # re-write as part of RDM-6847
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-037 @Ignore # re-write as part of RDM-6847
  Scenario: must return appropriate negative response when request does not provide an authorized access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-034
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-590
  Scenario: must return status 200 along with the case-view object successfully
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-027.01_GetToken]
    And a case that has just been created as in [S-027.01_Case]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains Last State Modified Date metadata field]
    And the response has all other details as expected
