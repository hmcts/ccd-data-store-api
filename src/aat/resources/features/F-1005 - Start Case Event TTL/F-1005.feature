@F-1005
Feature: F-1005: Update Case - Start Case Event - Update Code for TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1005.1 @AC1
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1005_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1005.6 @AC6
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1005_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1005.11 @AC11
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1005_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1005.12 @AC12
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1005_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1005.13 @AC13
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1005_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]
