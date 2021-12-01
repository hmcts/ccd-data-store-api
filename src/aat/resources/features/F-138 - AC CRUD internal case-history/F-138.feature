@F-138 @crud
Feature: F-138: Get Case History View Details with access Internal API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-138.1
  Scenario: User getting case history view with no CaseType R access gets error
    Given a case that has just been created as in [F-138_CreateCase],
    And a successful call [to get the details about case event for the case just created above] as in [F-138_GetEventId],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType with no R access],
    And it is submitted to call the [retrieve case history for the event] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 Not Found],
    And the response has all other details as expected.
