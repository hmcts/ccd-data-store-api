@F-1006
Feature: F-1006: Submit Event to Update TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-1006.1 @AC-1
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL and Override TTL less than Guard value and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
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

    @S-1006.2 @AC-2
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL and Override TTL are NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
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
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to greater than today + guard value]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

    @S-1006.4 @AC-7
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
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

    @S-1006.5 @AC-2
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL and Override TTL are NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
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

    @S-1006.6 @AC-3
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1006_CreateSuspendedCasePreRequisiteCaseworker]
     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1006_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has TTL.Suspension value changed to No from Yes]
      And the request [has TTL.OverrideTTL set to greater than today + guard value]
      And the request [has TTL.SystemTTL set to greater than today + guard value]
      And it is submitted to call the [submit event creation as case worker] operation of [CCD Data Store]
     Then a positive response is received
      And the response has all other details as expected

    @S-1006.7 @AC-7
    Scenario: TTL.Suspended changed to "N/No/F/False" or NULL, SystemTTL greater than TTLGuard, OverRide TTL is NULL and Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST
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
