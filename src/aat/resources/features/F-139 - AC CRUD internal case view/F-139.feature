@F-139 @crud
Feature: F-139: Get Case View Details with access Internal API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-139.1
  Scenario: User getting case history view with no CaseType R access gets error
    Given a case that has just been created as in [S-139.1_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseType with no R access],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 Not Found],
    And the response has all other details as expected.

  @S-139.2
  Scenario: User getting a case with no CaseField R access has those fields filtered out of response
    Given a case that has just been created as in [S-139.2_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a CaseField with no R access],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response has all other details as expected,
    And the response [does not contain the case field with no R CRUD access].

  @S-139.3
  Scenario: User getting a case with no CaseState R access gets an error
    Given a case that has just been created as in [S-139.2_CreateCase],
    And a successful call [to create a token for event creation] as in [S-139.3_Event_Token_Creation]
    And a successful call [to create event] as in [S-139.3_Event_Creation]
    And a user with [no R access to case state]
    When a request is prepared with appropriate values,
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 Not Found],
    And the response has all other details as expected.

  @S-139.4
  Scenario: User getting a case with no CaseEvent R access has that event filtered out of response
    Given a case that has just been created as in [S-139.2_CreateCase],
    And a successful call [to create a token for event creation] as in [S-139.4_Event_Token_Creation]
    And a successful call [to create event] as in [S-139.4_Event_Creation]
    And a user with [no R access to case event]
    When a request is prepared with appropriate values,
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response has all other details as expected,
    And the response [does not contain the case event with no R CRUD access].

  @S-139.5
  Scenario: User getting a case with no CaseType U access has the update events filtered out of response
    Given a case that has just been created as in [S-139.5_CreateCase],
    And a user with [no U access to case type]
    When a request is prepared with appropriate values,
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response has all other details as expected,
    And the response [does not contain the case events under triggers].
