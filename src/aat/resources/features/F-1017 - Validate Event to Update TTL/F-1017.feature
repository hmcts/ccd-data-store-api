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


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: Trigger a mid-event callback that makes permitted changes to the TTL values: v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.11 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (null -> missing). Mid Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (null -> missing)]
        And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [contains the TTL.OverrideTTL from the previouse data]
        And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

    @S-1017.12 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (No -> NO). Mid Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (No -> NO)]
        And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

    @S-1017.13 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (Yes -> YES). Mid Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (Yes -> YES)]
        And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


    @S-1017.15 #CCD-3562
    Scenario: Trigger a mid event callback that has TTL missing. Mid Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL missing]
        And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

    @S-1017.16 #CCD-3562
    Scenario: Trigger a mid event callback that changes TTL set to null. Mid Event is invoked on v1_external#/caseworker/case-details-endpoint/validateCaseDetailsUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL set to null]
        And it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

       Then a negative response is received
        And the response has all other details as expected
        And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: Trigger a mid-event callback that makes permitted changes to the TTL values: v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.21 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (null -> missing). Mid Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
      Given a user with [an active profile in CCD]
        And a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCitizen]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCitizen]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (null -> missing)]
        And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [contains the TTL.OverrideTTL from the previouse data]
        And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

    @S-1017.22 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (No -> NO). Mid Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
      Given a user with [an active profile in CCD]
        And a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCitizen]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCitizen]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (No -> NO)]
        And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

    @S-1017.23 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (Yes -> YES). Mid Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
      Given a user with [an active profile in CCD]
        And a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCitizen]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCitizen]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (Yes -> YES)]
        And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


    @S-1017.25 #CCD-3562
    Scenario: Trigger a mid event callback that has TTL missing. Mid Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
      Given a user with [an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCitizen]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL missing]
        And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

    @S-1017.26 #CCD-3562
    Scenario: Trigger a mid event callback that changes TTL set to null. Mid Event is invoked on v1_external#/citizen/case-details-endpoint/validateCaseDetailsUsingPOST_1
      Given a user with [an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCitizen]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL set to null]
        And it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]

       Then a negative response is received
        And the response has all other details as expected
        And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: Trigger a mid-event callback that makes permitted changes to the TTL values: v2_external#/case-data-validator-controller/validateUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @S-1017.31 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (null -> missing). Mid Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (null -> missing)]
        And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [contains the TTL.OverrideTTL from the previouse data]
        And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

    @S-1017.32 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (No -> NO). Mid Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (No -> NO)]
        And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

    @S-1017.33 #CCD-3535
    Scenario: Trigger a mid event callback that changes TTL.Suspended (Yes -> YES). Mid Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a user with [access to manage TTL properties]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
        And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that changes the TTL.OverrideTTL value (null -> missing)]
        And the request [is configured to trigger a mid event callback that changes the TTL.Suspended value (Yes -> YES)]
        And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
        And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
        And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


    @S-1017.35 #CCD-3562
    Scenario: Trigger a mid event callback that has TTL missing. Mid Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL missing]
        And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

       Then a positive response is received
        And the response has all other details as expected
        And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

    @S-1017.36 #CCD-3562
    Scenario: Trigger a mid event callback that changes TTL set to null. Mid Event is invoked on v2_external#/case-data-validator-controller/validateUsingPOST
      Given a user with [a caseworker with an active profile in CCD]
        And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

       When a request is prepared with appropriate values
        And the request [contains a case Id that has just been created as in CreateCase_TTLCaseType_PreRequisiteCaseworker]
        And the request [contains an event token for the case just created above]
        And the request [has a TTLIncrement of 20 days configured]
        And the request [is configured to trigger a mid event callback that responds with TTL set to null]
        And it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]

       Then a negative response is received
        And the response has all other details as expected
        And the response [contains the error message indicating unauthorised change to the TTL values]
