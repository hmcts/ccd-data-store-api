@F-116
Feature: F-116: Retrieve audit events by case ID (V2)

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-116.1
  Scenario: must return list of audit events successfully for a case
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a successful call [to create a token for case update event] as in [S-116.1-Prerequisite]
    And a successful call [to update the case] as in [S-116.1-UpdateCase]
    When a request is prepared with appropriate values
    And the request [contains the Id of the case just created]
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains all audit event details under the case]
    And the response has all other details as expected

  @S-116.2
  Scenario: must return an error response for a malformed Case ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an malformed case ID]
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an error message saying that the case ID is invalid]
    And the response has all other details as expected

  @S-116.3
  Scenario: must return an error response for a non-existing Case ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existent Case ID]
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an error message saying that the case is not found]
    And the response has all other details as expected
