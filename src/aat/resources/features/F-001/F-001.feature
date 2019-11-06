@F-001
Feature: get default settings for user from CCD Data Store

  @S-129
  Scenario: default settings for user should be returned when user exists
    Given an appropriate test context as detailed in the test data source
    And a user with a detailed profile in CCD
    When a request is prepared with appropriate values
    And it is submitted to call the get default settings for user operation of CCD Data Store
    Then a positive response is received
    And the response has all the details as expected
