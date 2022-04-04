@F-132 @crud
Feature: F-132: Get Events External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-132.2
    Scenario: User can only see the events in which they have specific CaseEvent R access to
      Given a case that has just been created as in [F-132_CreateCase],
      And a user [with no R access to an event]
      And a successful call [to create a token for event creation] as in [S-132.2_Event5_Token_Creation]
      And a successful call [to create event] as in [S-132.2_Event5_Creation]
      When a request is prepared with appropriate values,
      And it is submitted to call the [get case events] operation of [CCD Data Store],
      Then a positive response is received
      And the response has all other details as expected.
      And the response [only contains the event the user has R access to]
