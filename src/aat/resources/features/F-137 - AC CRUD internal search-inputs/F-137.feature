@F-137 @crud
Feature: F-137: Get Search Input Details with access Internal API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-137.1
  Scenario: User getting search inputs with no CaseType R access gets error
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType with no R access],
    And it is submitted to call the [retrieve search input details] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 Not Found],
    And the response has all other details as expected.

  @S-137.2
  Scenario: User getting search inputs with no CaseField R access has field filtered out of response
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType that has a field with no R access],
    And it is submitted to call the [retrieve search input details] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response has all other details as expected,
    And the response [does not contain the case field with no R CRUD access]

  @S-137.3
  Scenario: PrivateUser getting search inputs with no CaseType R access gets error
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType with no R access],
    And it is submitted to call the [retrieve search input details] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 Not Found],
    And the response has all other details as expected.

  @S-137.4
  Scenario: PrivateUser getting search inputs with no CaseField R access has field filtered out of response
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType that has a field with no R access],
    And it is submitted to call the [retrieve search input details] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response has all other details as expected,
    And the response [does not contain the case field with no R CRUD access]
