@F-048
Feature: Get the pagination metadata for a case data search for Case Worker

  @S-133
  Scenario: must return 400 if the sort direction in input parameters is not in ASC or DESC
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the get default settings for user operation of CCD Data Store
    Then a positive response is received
    And the response has all the details as expected
