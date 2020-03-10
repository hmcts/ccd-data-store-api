@F-042
Feature: F-042: Trigger "aboutToStart" event as a Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-243
  Scenario: Trigger the aboutToStart callback event for a caseworker for a new case which has not been started yet.
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Case_Creation_Using_Caseworker1_Role]
    When a request is prepared with appropriate values
    And the request [is prepared with a valid User ID, Jurisdiction, Case Type ID and Event Trigger ID and the Case ID just created]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the HTTP 200 OK return code]
    And the response [returns the START_EVENT trigger along with the event token]
    And the response has all other details as expected

  @S-246
  Scenario: Trigger the aboutToStart callback event for a caseworker for an invalid Case ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [is prepared with an invalid Case ID]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains the HTTP 400 Bad Request return code]
    And the response has all other details as expected

  @S-248
  Scenario: Trigger the aboutToStart callback event for a caseworker for an invalid Jurisdiction ID
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Case_Creation_Using_Caseworker1_Role]
    When a request is prepared with appropriate values
    And the request [is prepared with an invalid Jurisdiction ID]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-249
  Scenario: Return error code 422 when an event request could not be processed.
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Case_Creation_Using_Caseworker1_Role]
    And a successful call [to fire a START_PROGRESS event on the case just created] as in [S-249_Update_Case_State]
    When a request is prepared with appropriate values
    And the request [is prepared with an invalid START_EVENT]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 422 Forbidden]
    And the response has all other details as expected

  @S-244 @Ignore # re-write as part of RDM-6847
  Scenario: must return a negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Case_Creation_Using_Caseworker1_Role]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials in CCD]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-245 @Ignore # re-write as part of RDM-6847
  Scenario: must return a negative response when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Case_Creation_Using_Caseworker1_Role]
    When a request is prepared with appropriate values
    And the request [does not provide valid authorization credentials for an operation in CCD]
    And it is submitted to call the [Start the event creation process for a new case for a Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected
