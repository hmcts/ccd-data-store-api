@F-133 @crud
Feature: F-133: Get Case External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-133.1
  Scenario: User without CaseState Access R cannot see the case
    Given a case that has just been created as in [F-133_CreateCase],
    And a successful call [to create a token for event creation] as in [S-133.1_Event_Token_Creation]
    And a successful call [to create event] as in [S-133.1_Event_Creation]
    And a user with [no R access to case state]
    When a request is prepared with appropriate values,
    And the request [attempts to get the previously created case]
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected
    And the response [does not contain the previously created case]
