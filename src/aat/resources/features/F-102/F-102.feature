@F-102
Feature: Get the pagination metadata for a case data search for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-533
  Scenario: must return a list of jurisdictions for a valid user
    Given a user with [a detailed profile in CCD having create case access for a jurisdiction]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the list of jurisdictions a user has access to]
    And the response has all the details as expected
