@F-047
Feature: F-047: Get case ids

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-097
    Scenario: must return 200 and a list of case ids a user has access to
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a successful call [to grant access on a case] as in [F-047_Grant_Access]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get case ids] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a list of case ids, along with an HTTP-200 OK]
    And the response has all other details as expected
