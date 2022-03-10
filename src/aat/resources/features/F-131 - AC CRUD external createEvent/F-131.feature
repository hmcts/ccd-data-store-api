@F-131 @crud
Feature: F-131: Create Event External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-131.1
  Scenario: User cannot create event with missing CaseEvent C Access
    Given a case that has just been created as in [F-131_CreateCase],
    And a successful call [to create a token for event creation] as in [S-131.1_Token_Creation]
    And a user [with no C access to create an event]
    When a request is prepared with appropriate values,
    And it is submitted to call the [create event] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.


  @S-131.2
  Scenario: User cannot create event with missing CaseField C Access for a new field
    Given a case that has just been created as in [F-131_CreateCase],
    And a successful call [to create a token for event creation] as in [S-131.2_Token_Creation]
    And a user [with no C access to a field in the new event]
    When a request is prepared with appropriate values,
    And the request [attempts to create an event for the previously created case]
    And it is submitted to call the [create event] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.


  @S-131.4
  Scenario: User successfully updates a case with a field with no CaseField Access R does not see the field in the response
    Given a case that has just been created as in [F-131_CreateCase],
    And a user [with no R access for a field being created]
    And a successful call [to create a token for event creation] as in [S-131.4_Token_Creation]
    When a request is prepared with appropriate values,
    And the request [attempts to create an event for the previously created case]
    And it is submitted to call the [create event] operation of [CCD Data Store],
    Then a positive response is received
    And the response [does not display the newly created text field]
    And the response has all other details as expected.


  @S-131.5
  Scenario: User cannot create event with missing CaseType U Access
    Given a user [with no U access for the CaseType],
    And a successful call [to create a token for case creation] as in [S-131.5_CreateCase_Token_Creation]
    And a case that has just been created as in [F-131.5_CreateCase],
    And a successful call [to create a token for event creation] as in [S-131.5_Token_Creation]
    When a request is prepared with appropriate values,
    And the request [attempts to create an event for the previously created case]
    And it is submitted to call the [create event] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.
