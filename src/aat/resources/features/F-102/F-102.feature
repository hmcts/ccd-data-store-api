@F-102
Feature: Get the pagination metadata for a case data search for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-533
  Scenario: must return a list of jurisdictions for a valid user
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [is prepared with valid Jurisdiction, Case ID and User ID]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [returns the pagination metadata]
    And the response has all the details as expected
