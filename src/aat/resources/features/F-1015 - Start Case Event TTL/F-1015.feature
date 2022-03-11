@F-1008
Feature: F-1008: Update Case - Start Case Event - Update Code for TTL

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1008.1 @AC1
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on
            v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1008.2 @AC2
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on
  v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1008.3 @AC3
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked
            on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1008.4 @AC4
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked on
            v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL]
    And   it is submitted to call the [Fetch an update event trigger in the context of a case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today]

  @S-1008.5 @AC5
  Scenario: TTLIncrement is set to "20" for the Case Event and Start Event is invoked
  on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About To Start callback that does not change of the TTL.suspended or TTL.OverrideTTL or TTL.SystemTTL],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has been set to 20 days from today].

  @S-1008.6 @AC6
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on
            v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1008.7 @AC7
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is invoked on
            v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details] 
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Start event creation process to update a case as Citizen] operation of [CCD Data Store]
    Then  a positive response is received 
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1008.8 @AC8
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is
            invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details] 
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a positive response is received 
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1008.9 @AC9
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is
          invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values
    And   the request [contains correctly configured event details] 
    And   the request [has a null TTLIncrement configured]
    And   the request [is configured to trigger an About to Start callback]
    And   it is submitted to call the [Fetch an update event trigger in the context of a case] operation of [CCD Data Store]
    Then  a positive response is received 
    And   the response has all other details as expected
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified]

  @S-1008.10 @AC10
  Scenario: TTLIncrement is blank (Null) for the Case Event and Start Event is
            invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a null TTLIncrement configured],
    And   the request [is configured to trigger an About to Start callback],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response has all other details as expected,
    And   the response [contains the TTL.SystemTTL for the case, that has not been modified].

  @S-1008.11 @AC11
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback.
            Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.12 @AC12
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback.
            Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.13 @AC13
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback.
            Start Event is invoked on v1_external#/case-details-endpoint/startEventForCaseworkerUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.14 @AC14
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback.
  Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a negative response is received,
    And   the response has all other details as expected,
    And   the response [contains the error message indicating unauthorised change to the TTL values].

  @S-1008.15 @AC15
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback.
  Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a negative response is received,
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values].

  @S-1008.16 @AC16
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback.
  Start Event is invoked on v2_internal#/ui-start-trigger-controller/getCaseUpdateViewEventUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker],
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details],
    And   the request [has a TTLIncrement of 20 days configured],
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended],
    And   it is submitted to call the [Retrieve an start event trigger for case] operation of [CCD Data Store],
    Then  a negative response is received,
    And   the response has all other details as expected,
    And   the response [contains the error message indicating unauthorised change to the TTL values].


  @S-1008.17 @AC17
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback.
            Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.18 @AC18
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback.
            Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.19 @AC19
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback.
            Start Event is invoked on v2_external#/start-event-controller/getStartEventTriggerUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.20 @AC20
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback.
  Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Start event creation process to update a case as a Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.21 @AC21
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback.
  Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Start event creation process to update a case as a Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.22 @AC22
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback.
  Start Event is invoked on v1_external#/case-details-endpoint/startEventForCitizenUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizen]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Start event creation process to update a case as a Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.23 @AC23
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.OverrideTTL has changed after About to Start Callback.
  Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.OverrideTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.24 @AC24
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.SystemTTL has changed after About to Start Callback.
  Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET
    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.SystemTTL]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]

  @S-1008.25 @AC25
  Scenario: TTLIncrement is set to "20" for the Case Event, TTL.suspended has changed after About to Start Callback.
  Start Event is invoked on v1_internal#/query-endpoint/getEventTriggerForCaseUsingGET

    Given a user with [an active profile in CCD],
    And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworker]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [has a TTLIncrement of 20 days configured]
    And   the request [is configured to trigger an About to Start callback that changes TTL.Suspended]
    And   it is submitted to call the [Retrieve an update event trigger for case] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all other details as expected
    And   the response [contains the error message indicating unauthorised change to the TTL values]
