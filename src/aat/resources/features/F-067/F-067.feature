@F-067
Feature: F-067: Retrieve a CaseView Event by case and event id for dynamic display

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-212
  Scenario: should retrieve case view when the case reference and case event exists
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Befta_Default_Full_Case_Creation_Data]
    And a successful call [to get the details about case event for the case just created] as in [some file]
    When a request is prepared with appropriate values
    And the request [contains the reference of the case just created and the event id valid for that case]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has Http 200 OK]
    And the response has all other details as expected

  @S-249 @Ignore
  Scenario: Return error code 422 when an event request could not be processed.
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Befta_Default_Full_Case_Creation_Data]
    And a successful call [to fire a START_PROGRESS event on the case just created] as in [S-249_Update_Case_State]
    When a request is prepared with appropriate values
    And the request [is prepared with an invalid START_EVENT]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 422 Forbidden]
    And the response has all other details as expected
