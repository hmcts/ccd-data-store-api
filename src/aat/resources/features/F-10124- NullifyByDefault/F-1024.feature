@F-1024
Feature: F-1024: Update Case - Start Case Event - NullifyByDefault

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1024.1
  Scenario: Applying NullifyByDefault at start of case creation for v1_external#/case-details-endpoint/startCaseForCaseworkerUsingGET
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL values it is still null]
    And   it is submitted to call the [Start event creation as Case worker] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.2
  Scenario: Applying NullifyByDefault at start of case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL values it is still null]
    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.3
  Scenario: Applying NullifyByDefault at start of case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_StartCaseTrigger_Case_Creation]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has TTL value is still null]
    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.4
  Scenario: About to start callback changes the value of TTL during case creation for v1_external#/case-details-endpoint/startCaseForCaseworkerUsingGET
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has changed value of the TTL values and is not null]
    And   it is submitted to call the [Start event creation as Case worker] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.5
  Scenario: About to start callback changes the value of TTL during case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has changed value of the TTL values and is not null]
    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.6 @Ignore #Review
  Scenario: About to start callback changes the value of TTL during case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_StartCaseTrigger_Case_Creation_NotNull]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has TTL value is not null]
    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.7
  Scenario: Value of TTL is not null before case creation for v1_external#/case-details-endpoint/startCaseForCaseworkerUsingGET
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And   the request [contains TTL value is not null]
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has TTL value is still not null]
    And   it is submitted to call the [Start event creation as Case worker] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.8
  Scenario: Value of TTL is not null before case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And   the request [contains TTL value is not null]
    And   the request [contains correctly configured event details]
    And   the request [is configured to trigger an About To Start callback that has TTL value is still not null]
    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.9 @Ignore #Review
  Scenario: Value of TTL is not null before case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_StartCaseTrigger_Case_Creation_NotNull]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About To Start callback that has TTL value is not null]
    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.10
  Scenario: During validation, adjust any validation on TTL for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCaseworker_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.11
  Scenario: During validation, adjust any validation on TTL for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.12
  Scenario: During validation, adjust any validation on TTL for v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.13
  Scenario: During validation, Mid-event callback updates TTL value for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCaseworker_MidEventUpdate]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.14  @Ignore # Review
  Scenario: During validation, Mid-event callback updates TTL value for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEventUpdate]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 400 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.15
  Scenario:  During validation, Mid-event callback updates TTL value for v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEventUpdate]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.16
  Scenario: Value of TTL is not null before validation for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCaseworker_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.17
  Scenario: Value of TTL is not null before validation for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.18
  Scenario: Value of TTL is not null before validation for v2_external#/case-data-validator-controller/validateUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_MidEvent]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.19 @Ignore # Review
  Scenario: During Submit case creation adjust any validation on TTL for  v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCaseworker_AboutToSubmit]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [Submit event creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.20 @Ignore # Review
  Scenario: During Submit case creation adjust any validation on TTL for v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_AboutToSubmit]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [Submit event creation as citizen (v1_ext citizen)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.21 @Ignore # Revieww
  Scenario:  During Submit case creation adjust any validation on TTL for v2_external#/case-controller/createCaseUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_AboutToSubmit]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
    And   it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]
    Then  a positive response is received,
    And   the response [has the 200 OK code]
    And   the response has all other details as expected
    And   the response [contains the TTL set to null]

  @S-1024.22 @Ignore # Review
  Scenario: During Submit case creation About to submit callback updates TTL value for v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCaseworker_AboutToSubmit_NotNull]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [Submit event creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.23 @Ignore # Review
  Scenario: During Submit case creation About to submit callback updates TTL value for v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_AboutToSubmit_NotNull]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [Submit event creation as citizen (v1_ext citizen)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

  @S-1024.24 @Ignore # Review
  Scenario: During Submit case creation About to submit callback updates TTL value for v2_external#/case-controller/createCaseUsingPOST
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1024_CreateCasePreRequisiteCitizen_AboutToSubmit_NotNull]
    When  a request is prepared with appropriate values,
    And   the request [contains correctly configured event details for case C1]
    And   the request [contains TTL value is not null]
    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
    And   it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 OK code]
    And   the response has all other details as expected

