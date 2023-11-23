@F-1021
Feature: F-1021: Retrieve audit events by case ID when case created with onBehalfOfUserId

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1021.1
  Scenario: must return list of audit events successfully for a create case
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-1021_Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [contains the Id of the case just created]
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains all audit event details under the case]
    And the response has all other details as expected


