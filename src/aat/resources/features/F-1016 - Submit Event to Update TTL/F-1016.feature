@F-1016
Feature: F-1016: Submit Event to Update TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-1016.1 #AC-1
    Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
      And the request [has TTL.SystemTTL set to less than today + TTL Guard]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1016_GetCaseDetails_Caseworker]

    @S-1016.1.repeat
    Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [is a repeat of S-1016.1 but with TTL.Suspended set to null]
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to NULL from Yes]
      And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
      And the request [has TTL.SystemTTL set to less than today + TTL Guard]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1016_GetCaseDetails_Caseworker]

    @S-1016.2 #AC-2
    Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to null]
      And the request [has TTL.SystemTTL set to null]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

    @S-1016.2.repeat
    Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [is a repeat of S-1016.2 but with TTL.Suspended set to null]
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to NULL from Yes]
      And the request [has TTL.OverrideTTL set to null]
      And the request [has TTL.SystemTTL set to null]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

    @S-1016.3 #AC-3
    Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverrideTTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to null]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

  @S-1016.4 #AC-4
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.5 #AC-5
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.6 #AC-6
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to Yes from No]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.7 #AC-7 #AC-8 #AC-9
    Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL greater than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspended value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to greater than today + guard value]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And the request [callback About to submit changes TTL.Suspended value]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
     Then a negative response is received
      And the response has all other details as expected

  @S-1016.8 #AC-1 #AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1016_GetCaseDetails_Caseworker]


  @S-1016.8.repeat
  Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [is a repeat of S-1016.8 but with TTL.Suspended set to null]
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to NULL from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And another call [to verify that the TTL.Suspended value has not changed in the database] will get the expected response as in [F-1016_GetCaseDetails_Caseworker]

  @S-1016.9 #AC-2 #AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.9.repeat
  Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [is a repeat of S-1016.9 but with TTL.Suspended set to null]
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to NULL from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.10 #AC-3 #AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverrideTTL is NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.11 #AC-4 #AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.12 #AC-5 #AC-11
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.13 #AC-6 #AC-11
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to Yes from No]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.14 #AC-7 #AC-8 #AC-9 #AC-11
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL greater than Guard value and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And the request [callback About to submit changes TTL.Suspended value]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1016.15 #AC-1 #AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And a successful call [to verify that the TTL.Suspended value has not changed in the database] as in [F-1016_GetCaseDetails_Citizen]

  @S-1016.15.repeat
  Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [is a repeat of S-1016.15 but with TTL.Suspended set to null]
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to NULL from Yes]
    And the request [has TTL.OverrideTTL set to less than today + TTL Guard]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected
    And a successful call [to verify that the TTL.Suspended value has not changed in the database] as in [F-1016_GetCaseDetails_Citizen]

  @S-1016.16 #AC-2 #AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.16.repeat
  Scenario: TTL.Suspended changed to null, SystemTTL and OverrideTTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [is a repeat of S-1016.16 but with TTL.Suspended set to null]
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to NULL from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.17 #AC-3 #AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL greater than TTLGuard, OverrideTTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to null]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.18 #AC-4 #AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL is NULL, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to null]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.19 #AC-5 #AC-10
  Scenario:  TTL.Suspended changed to "No", SystemTTL is less than TTLGuard, OverrideTTL is greater than TTLGuard and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.SystemTTL set to less than today + TTL Guard]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.20 #AC-6 #AC-10
  Scenario:  TTL.Suspended changed to "Yes" and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to Yes from No]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1016.21 #AC-7 #AC-8 #AC-9 #AC-10
  Scenario: TTL.Suspended changed to "No", SystemTTL and OverrideTTL greater than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1016_CreateSuspendedCasePreRequisiteCitizen]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1016_CreateSuspendedCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [has TTL.Suspended value changed to No from Yes]
    And the request [has TTL.OverrideTTL set to greater than today + guard value]
    And the request [has TTL.SystemTTL set to greater than today + guard value]
    And the request [callback About to submit changes TTL.Suspended value]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# CCD-3476: TTL Increment tests when data not present in event data or permission restricted: v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1016.51 #CCD-3476
    Scenario: Set TTL for first time when TTL data not present in event data and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will create a TTL value using TTL increment of 30 days]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

    @S-1016.52 #CCD-3476
    Scenario: Update TTL value when TTL data not present in event data and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will update the existing TTL value with TTL increment of 30 days]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

    @S-1016.53 #CCD-3476
    Scenario: Attempt to update SystemTTL without permissions and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.SystemTTL value set to a valid date]
      And the request [will fail due to lack of permissions to TTL field]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL data is unchanged] will get the expected response as in [S-1016.53.VerifyTtlUnchanged]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# CCD-3476: TTL Increment tests when data not present in event data or permission restricted: v2_external#/case-controller/createEventUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1016.61 #CCD-3476
    Scenario: Set TTL for first time when TTL data not present in event data and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will create a TTL value using TTL increment of 30 days]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

    @S-1016.62 #CCD-3476
    Scenario: Update TTL value when TTL data not present in event data and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will update the existing TTL value with TTL increment of 30 days]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

    @S-1016.63 #CCD-3476
    Scenario: Attempt to update SystemTTL without permissions and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.SystemTTL value set to a valid date]
      And the request [will fail due to lack of permissions to TTL field]
      And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL data is unchanged] will get the expected response as in [S-1016.63.VerifyTtlUnchanged]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# CCD-3476: TTL Increment tests when data not present in event data or permission restricted: v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1016.71 #CCD-3476
    Scenario: Set TTL for first time when TTL data not present in event data and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will create a TTL value using TTL increment of 30 days]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And another call [to verify that the TTL data has changed] will get the expected response as in [S-1016.71.VerifyCaseDetails]

    @S-1016.72 #CCD-3476
    Scenario: Update TTL value when TTL data not present in event data and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has no TTL data present in the submitted data]
      And the request [will update the existing TTL value with TTL increment of 30 days]
      And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And another call [to verify that the TTL data has changed] will get the expected response as in [S-1016.72.VerifyCaseDetails]

    @S-1016.73 #CCD-3476
    Scenario: Attempt to update SystemTTL without permissions and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]
      And a successful call [to grant access to a case] as in [F-1016_GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
      And a successful call [to set TTL properties for a case] as in [F-1016_UpdateCase_TTLCaseType_manageCaseTTL_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1016_CreateCase_TTLCaseType_PreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.SystemTTL value set to a valid date]
      And the request [will fail due to lack of permissions to TTL field]
      And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And another call [to verify that the TTL data is unchanged] will get the expected response as in [S-1016.73.VerifyTtlUnchanged]

