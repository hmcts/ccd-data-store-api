@F-1006
Feature: F-1006: Submit Event to Update TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-1006.1 @AC-1
    Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
      And the request [has TTL.SystemTTL set to less than today + TTL Guard]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1006_GetCaseDetails_Caseworker]

    @S-1006.2 @AC-2
    Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to null]
      And the request [has TTL.SystemTTL set to null]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

    @S-1006.3 @AC-3
    Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to null]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

  @S-1006.4 @AC-4
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.5 @AC-5
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.6 @AC-6
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to Yes from No]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.7 @AC-7 @AC-8 @AC-9
    Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to greater than today + guard value]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And the request [callback About to submit changes TTL.Suspended value]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a negative response is received
      And the response has all other details as expected

  @S-1006.8 @AC-1 @AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL less than Guard value and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1006_GetCaseDetails_Caseworker]

  @S-1006.9 @AC-2 @AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL are NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.10 @AC-3 @AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.11 @AC-4 @AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.12 @AC-5 @AC-11
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.13 @AC-6 @AC-11
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to Yes from No]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.14 @AC-7 @AC-8 @AC-9 @AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And the request [callback About to submit changes TTL.Suspended value]
    And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1006.15 @AC-1 @AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And a successful call [to verify that the TTL.Suspended value has not changed in the database] as in [F-1006_GetCaseDetails_Citizen]

  @S-1006.16 @AC-2 @AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL and Override TTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.17 @AC-3 @AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.18 @AC-4 @AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.19 @AC-5 @AC-10
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverRide TTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.20 @AC-6 @AC-10
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to Yes from No]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1006.21 @AC-7 @AC-8 @AC-9 @AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspension value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And the request [callback About to submit changes TTL.Suspended value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

