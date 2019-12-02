@F-048
Feature: F-048: Get the pagination metadata for a case data search for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-136
  Scenario: must return pagination metadata successfully for correct inputs
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [is prepared with valid Jurisdiction, Case ID and User ID]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [returns the pagination metadata]
    And the response has all the details as expected

  @S-133
  Scenario: must return 400 if the sort direction in input parameters is not in ASC or DESC
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid Sort Direction]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an error message : Unknown sort direction]
    And the response has all the details as expected

  @S-134
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid authentication credentials]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-135
  Scenario: must return negative response when request provides authentic credentials without authorized access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [provides authentic credentials without authorized access to the operation]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-515
  Scenario: must return 400 when casefields do not start with “case.”
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains case fields that do not start with case.]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an error message : unknown metadata search parameters]
    And the response has all the details as expected

  @S-516
  Scenario: must return 400 when security classification in input parameters is present and invalid
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid security classification]
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an error message : unknown security classification]
    And the response has all the details as expected
