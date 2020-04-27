@F-103
Feature: F-103: Retrieve audit events by case ID (V2)

    Background: Load test data for the scenario
      Given an appropriate test context as detailed in the test data source

    @S-597
        Scenario: must return list of audit events successfully for a case
        Given a user with [an active profile in CCD],
        And a case that has just been created as in [Standard_Full_Case_Creation_Data],
        And a successful call [to get an event token for the case just created] as in [S-597-Prerequisite],
        When a request is prepared with appropriate values,
        And the request [contains the Id of the case just created],
        And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
        Then a positive response is received,
        And the response [contains all audit event details under the case],
        And the response has all other details as expected.

    @S-598 @Ignore
        Scenario: must return an error response for a malformed Case ID
        Given a user with [an active profile in CCD], 
        When a request is prepared with appropriate values, 
        And the request [contains an malformed case ID],
         And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store], 
        Then a negative response is received,
        And the response has all other details as expected.

    @S-599 @Ignore
        Scenario: must return an error response for a non-existing Case ID
        Given a user with [an active profile in CCD],
        When a request is prepared with appropriate values,
        And the request [contains a non-existent Case ID],
        And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
        Then a negative response is received,
        And the response has all other details as expected.
