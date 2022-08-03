@F-1015
Feature: F-1015: Update Case - Start Case Event - Update Code for TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL: (positive response)
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.1 #AC-1
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.1.repeat 
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker_noTTL]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   the request [is a repeat of S-1015.1 but with no TTL in create case call]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.2 #AC-2
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.2.repeat
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen_noTTL]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   the request [is a repeat of S-1015.2 but with no TTL in create case call]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.3 #AC-3
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.3.repeat
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker_noTTL]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   the request [is a repeat of S-1015.3 but with no TTL in create case call]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.4 #AC-4
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.4.repeat
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker_noTTL]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ]
    And   the request [is a repeat of S-1015.4 but with no TTL in create case call]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.5 #AC-5
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ],
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today].

  @S-1015.5.repeat
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker_noTTL],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL ],
    And   the request [is a repeat of S-1015.5 but with no TTL in create case call]
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is blank (Null): trigger an About To Start callback that does not change any of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.6 #AC-6
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1015.7 #AC-7
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1015.8 #AC-8
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1015.9 #AC-9
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1015.10 #AC-10
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a null TTLIncrement configured],
    And   the request [is configured to trigger an About to Start callback],
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that changes values: Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.11 #AC-11
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.12 #AC-12
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.13 #AC-13
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that changes values: (negative response) Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.14 #AC-14
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL],
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a negative response is received,
    And   the response has all other details as expected,
    And   the response [contains the error message indicating unauthorised change to the TTL values].

  @S-1015.15 #AC-15
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL],
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a negative response is received,
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values].

  @S-1015.16 #AC-16
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended],
    And   it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

    Then  a negative response is received,
    And   the response has all other details as expected,
    And   the response [contains the error message indicating unauthorised change to the TTL values].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that changes values: (negative response) Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.17 #AC-17
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.18 #AC-18
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.19 #AC-19
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that changes values: (negative response) Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.20 #AC-20
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.21 #AC-21
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.22 #AC-22
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# TTLIncrement is set to "20": trigger an About To Start callback that changes values: (negative response) Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.23 #AC-23
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]
    
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.24 #AC-24
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.25 #AC-25
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]

    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: TTLIncrement is set to "20": trigger an About To Start callback that makes permitted changes to the TTL values: v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.31 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (null -> missing). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (null -> missing)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [contains the TTL.OverrideTTL from the previouse data]
      And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

  @S-1015.32 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (No -> NO). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (No -> NO)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

  @S-1015.33 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (Yes -> YES). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (Yes -> YES)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


  @S-1015.35 #CCD-3562
  Scenario: Trigger a start event callback that has TTL missing. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL missing]
      And it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.36 #CCD-3562
  Scenario: Trigger a start event callback that changes TTL set to null. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL set to null]
      And it is submitted to call the [Start event creation process to update a case (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: TTLIncrement is set to "20": trigger an About To Start callback that makes permitted changes to the TTL values: v1_external#/case-details-endpoint/startEventForCitizenUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.41 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (null -> missing). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (null -> missing)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

  @S-1015.42 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (No -> NO). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (No -> NO)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

  @S-1015.43 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (Yes -> YES). Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD]
      And a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCitizen]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (Yes -> YES)]
      And it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected


  @S-1015.45 #CCD-3562
  Scenario: Trigger a start event callback that has TTL missing. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL missing]
      And it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

    Then a positive response is received
      And the response has all other details as expected

  @S-1015.46 #CCD-3562
  Scenario: Trigger a start event callback that changes TTL set to null. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCitizen]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL set to null]
      And it is submitted to call the [Start event creation process to update a case (v1_ext citizen)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: TTLIncrement is set to "20": trigger an About To Start callback that makes permitted changes to the TTL values: v2_external#/start-event-controller/getStartEventTriggerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.51 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (null -> missing). Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (null -> missing)]
      And it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [contains the TTL.OverrideTTL from the previouse data]
      And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

  @S-1015.52 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (No -> NO). Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (No -> NO)]
      And it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

  @S-1015.53 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (Yes -> YES). Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (Yes -> YES)]
      And it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


  @S-1015.55 #CCD-3562
  Scenario: Trigger a start event callback that has TTL missing. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL missing]
      And it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.56 #CCD-3562
  Scenario: Trigger a start event callback that changes TTL set to null. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL set to null]
      And it is submitted to call the [Start event creation process to update a case (v2_ext)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: TTLIncrement is set to "20": trigger an About To Start callback that makes permitted changes to the TTL values: v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.61 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (null -> missing). Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (null -> missing)]
      And it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [contains the TTL.OverrideTTL from the previouse data]
      And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

  @S-1015.62 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (No -> NO). Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (No -> NO)]
      And it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

  @S-1015.63 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (Yes -> YES). Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (Yes -> YES)]
      And it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


  @S-1015.65 #CCD-3562
  Scenario: Trigger a start event callback that has TTL missing. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL missing]
      And it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.66 #CCD-3562
  Scenario: Trigger a start event callback that changes TTL set to null. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL set to null]
      And it is submitted to call the [Start event creation process to update a case (v1_int caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected
      And the response [contains the error message indicating unauthorised change to the TTL values]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  #CCD-3535 & #CCD-3562: TTLIncrement is set to "20": trigger an About To Start callback that makes permitted changes to the TTL values: v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1015.71 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (null -> missing). Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNull_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (null -> missing)]
      And it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [contains the TTL.OverrideTTL from the previouse data]
      And the response [does not contain the TTL.Suspended as removed by callback (null -> missing)]

  @S-1015.72 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (No -> NO). Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedNo_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (No -> NO)]
      And it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (No -> NO)]

  @S-1015.73 #CCD-3535
  Scenario: Trigger a start event callback that changes TTL.Suspended (Yes -> YES). Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a user with [access to manage TTL properties]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]
      And a successful call [to grant access to a case] as in [GrantAccess_TTLCaseType_manageTTLUser_PreRequisiteCaseworker]
      And a successful call [to set TTL properties for a case] as in [UpdateCase_TTLCaseType_manageCaseTTL_SuspenedYes_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that changes the TTL.OverrideTTL value (null -> missing)]
      And the request [is configured to trigger an About To Start callback that changes the TTL.Suspended value (Yes -> YES)]
      And it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]
      And the response [does not contain the TTL.OverrideTTL as removed by callback (null -> missing)]
      And the response [contains the adjusted TTL.Suspended from the callback (Yes -> YES)]


  @S-1015.75 #CCD-3562
  Scenario: Trigger a start event callback that has TTL missing. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL missing]
      And it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

     Then a positive response is received
      And the response has all other details as expected
      And the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1015.76 #CCD-3562
  Scenario: Trigger a start event callback that changes TTL set to null. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [a caseworker with an active profile in CCD]
      And a successful call [to create a case] as in [CreateCase_TTLCaseType_PreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains correctly configured event details]
      And the request [has a TTLIncrement of 20 days configured]
      And the request [is configured to trigger an About To Start callback that responds with TTL set to null]
      And it is submitted to call the [Start event creation process to update a case (v2_int)] operation of [CCD Data Store],

     Then a negative response is received
      And the response has all other details as expected
      And the response [contains the error message indicating unauthorised change to the TTL values]

