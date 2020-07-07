@F-000 @Smoke
Feature: [SAMPLE] Get default settings for user

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-000
  Scenario: [SAMPLE] must return default user setting successfully for a user having a profile in CCD
    Given a successful call [to retrieve a user profile] as in [S-000-Prerequisite]
    And a user with [a detailed profile in CCD]
    And a case that has just been created as in [Befta_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [uses a uid that exists in IDAM]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected
