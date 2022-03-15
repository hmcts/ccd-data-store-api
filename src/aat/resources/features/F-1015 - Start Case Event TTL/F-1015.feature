@F-1015
Feature: F-1015: Update Case - Start Case Event - Update Code for TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1015.1 #AC-1
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
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
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
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
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Fetch an update event trigger in the context of a case] operation of [CCD Data Store]
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
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today].

  @S-1015.6 #AC-6
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Fetch an update event trigger in the context of a case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified].

  @S-1015.11 #AC-11
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.14 #AC-14
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
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
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
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
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a negative response is received,
    And   the response has all other details as expected,
    And   the response [contains the error message indicating unauthorised change to the TTL values].


  @S-1015.17 #AC-17
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.20 #AC-20
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
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
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
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
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1015.23 #AC-23
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback. Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1015_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
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
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]
