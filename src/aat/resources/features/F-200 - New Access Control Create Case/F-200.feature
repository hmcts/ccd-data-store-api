@F-200 @ra
Feature: Create first scenarios for new Access Control to run on the pipeline as a spike


  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


  @S-200.3
  Scenario: User's role assignments' SC lower than required for CaseType cannot create case
    Given a user with [a role assignment with SC PUBLIC for CaseType CT1 which is PRIVATE]
    And a successful call [to create a token for case creation] as in [CT1_GetToken]
    When a request is prepared with appropriate values
    And the request [attempts to create a case for CT1]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-200.4
  Scenario: User's role assignments' granting access required for CaseType can successfully create case
    Given a user with [a role assignment to access CaseType CT2]
    And a successful call [to create a token for case creation] as in [CT2_GetToken]
    When a request is prepared with appropriate values
    And the request [attempts to create a case for CT2]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected
