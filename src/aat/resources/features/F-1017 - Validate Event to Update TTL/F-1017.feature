@F-1017
Feature: F-1017: Validate Event to Update TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.1 #AC-1
    Scenario: TTL.Suspended changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.Suspended value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.2 #AC-2
    Scenario: TTL.SystemTTL changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.SystemTTL value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.3 #AC-3
    Scenario: TTL.OverrideTTL changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.OverrideTTL value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.4 #AC-4
    Scenario: TTL.Suspended changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.Suspended value changed]
      And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.5 #AC-5
    Scenario: TTL.SystemTTL changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.SystemTTL value changed]
      And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.6 #AC-6
    Scenario: TTL.OverrideTTL changed after returning from Mid Event Callback and Validate Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCitizen]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.OverrideTTL value changed]
      And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v2_external#/case-data-validator-controller/validateUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.7 #AC-7
    Scenario: TTL.Suspended changed after returning from Mid Event Callback and Validate Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.Suspended value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.8 #AC-8
    Scenario: TTL.SystemTTL changed after returning from Mid Event Callback and Validate Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.SystemTTL value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

    @S-1017.9 #AC-9
    Scenario: TTL.OverrideTTL changed after returning from Mid Event Callback and Validate Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
      And a successful call [to create a case] as in [F-1017_CreateSuspendedCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1017_CreateSuspendedCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [has the mid event callback change the TTL.OverrideTTL value changed]
      And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
