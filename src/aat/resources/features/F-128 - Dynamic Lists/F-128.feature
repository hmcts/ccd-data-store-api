@F-128
Feature: F-128: Dynamic Radio List ad Dynamic Multi Select List

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-128.1 @Ignore
  Scenario: must successfully create a case with a case type containing dynamic radio and dynamic multi select list
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation with about start event callback] as in [F-128_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains dynamic radio and dynamic multi select lists and Complex and collections]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

