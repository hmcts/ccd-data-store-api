@F-051
Feature: Get default setting for user

  @S-110
  Scenario: must return default user setting successfully for a user having a profile in CCD
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the get default settings for user operation of CCD Data Store
    Then a positive response is received
    And the response has all the details as expected
