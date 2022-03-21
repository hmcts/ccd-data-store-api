@F-135 @crud
Feature: F-135: Retrieve a Start Event Trigger Internal API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-135.1
  Scenario: User getting event trigger for Case with no event C access gets error
    Given a user [with no C access to create an event]
    And a case that has just been created as in [F-135_CreateCase],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Retrieve a start event trigger by Event ID] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains HTTP 404 status code],
    And the response has all other details as expected.

  @S-135.2
  Scenario: User getting event trigger for Case with no caseState U access for current case state gets error
    Given a case that has just been created as in [F-135_CreateCase],
    And a successful call [to create a token for an event creation] as in [S-135.2_Token_Creation]
    And the request [attempts to create an event for the previously created case],
    And a successful call [to create an event] as in [S-135.2_CreateEvent]
    And a user [with no U access for the current case state],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Retrieve a start event trigger by Event ID] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains HTTP 404 status code],
    And the response has all other details as expected.


  @S-135.3
  Scenario: User getting event trigger for Case with no CaseType U access gets error
    Given a successful call [to create a token for case creation] as in [S-131.5_CreateCase_Token_Creation]
    And a case that has just been created as in [F-135.3_CreateCase],
    And a user [with no case type U access to update a case],
    When a request is prepared with appropriate values,
    And the request [attempts to get event trigger for the previously created case],
    And it is submitted to call the [Retrieve a start event trigger by Event ID] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains HTTP 404 status code],
    And the response has all other details as expected.


