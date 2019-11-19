@F-000
Feature: [SAMPLE] Get default settings for user

  @S-000
  Scenario: [SAMPLE] must return default user setting successfully for a user having a profile in CCD
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses a uid that exists in IDAM]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected
