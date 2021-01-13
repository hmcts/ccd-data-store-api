@F-110
Feature: F-110: Create case for caseworker using V2 api

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-110.1 @Ignore
  Scenario: must validate date in a right format
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-109_GetToken]
    When a request is prepared with appropriate values
    And the request [contains valid value for a formatted Date field]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has 201 return code]
    And the response has all other details as expected

  @S-110.2 @Ignore
  Scenario: must return an error for date value with invalid format
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-109_GetToken]
    When a request is prepared with appropriate values
    And the request [contains Date field with incorrect format]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has 422 return code]
    And the response has all other details as expected
