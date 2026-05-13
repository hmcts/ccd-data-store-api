@F-1029
Feature: F-1029: Update Case - Start Case Event - DefaultFocus

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1029.4
  Scenario: Successful response for caseType id and having newly added display_focus column
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And  the request [contains valid caseType id which has display_focus value set for tab]
    And it is submitted to call the [GET /api/display/tab-structure/{id}] operation of [CCD Data Store]
    Then  a positive response is received,
    And in response with [200 success]and [All tabs for the caseType id is present in the response along with newly added display_focus value]

  @S-1029.5
  Scenario: Successful response for caseType id and not having display_focus column
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And  the request [contains valid caseType id which has no display_focus value set for tab]
    And it is submitted to call the [GET /api/display/tab-structure/{id}] operation of [CCD Data Store]
    Then  a positive response is received,
    And in response with [200 success]and [All tabs for the caseType id is present in the response along with newly added display_focus value]


#  @S-1029.2
#  Scenario: Applying NullifyByDefault at start of case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that does not change any of the TTL values it is still null]
#    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#
#  @S-1029.3
#  Scenario: Applying NullifyByDefault at start of case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_StartCaseTrigger_Case_Creation]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has TTL value is still null]
#    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
#    Then  a positive response is received
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.4
#  Scenario: About to start callback changes the value of TTL during case creation for v1_external#/case-details-endpoint/startCaseForCaseworkerUsingGET
#    Given a user with [an active profile in CCD]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has changed value of the TTL values and is not null]
#    And   it is submitted to call the [Start event creation as Case worker] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.5
#  Scenario: About to start callback changes the value of TTL during case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has changed value of the TTL values and is not null]
#    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.6
#  Scenario: About to start callback changes the value of TTL during case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_StartCaseTrigger_Case_Creation_NotNull]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has TTL value is not null]
#    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_6_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.7
#  Scenario: Value of TTL is not null before case creation for v1_external#/case-details-endpoint/startCaseForCaseworkerUsingGET
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values,
#    And   the request [contains TTL value is not null]
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has TTL value is still not null]
#    And   it is submitted to call the [Start event creation as Case worker] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.8
#  Scenario: Value of TTL is not null before case creation for v1_external#/case-details-endpoint/startCaseForCitizenUsingGET
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values,
#    And   the request [contains TTL value is not null]
#    And   the request [contains correctly configured event details]
#    And   the request [is configured to trigger an About To Start callback that has TTL value is still not null]
#    And   it is submitted to call the [Start event creation as Citizen] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.9
#  Scenario: Value of TTL is not null before case creation for v2_external#/start-event-controller/getStartCaseTriggerUsingGET
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_StartCaseTrigger_Case_Creation_NotNull]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About To Start callback that has TTL value is not null]
#    And   it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_6_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.10
#  Scenario: During validation, adjust any validation on TTL for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCaseworker_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#
#  @S-1029.11
#  Scenario: During validation, adjust any validation on TTL for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_11_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.12
#  Scenario: During validation, adjust any validation on TTL for v2_external#/case-data-validator-controller/validateUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_11_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.13
#  Scenario: During validation, Mid-event callback updates TTL value for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCaseworker_MidEventUpdate]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.14
#  Scenario: During validation, Mid-event callback updates TTL value for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEventUpdate]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_14_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.15
#  Scenario:  During validation, Mid-event callback updates TTL value for v2_external#/case-data-validator-controller/validateUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEventUpdate]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_14_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.16
#  Scenario: Value of TTL is not null before validation for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCaseworker_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.17
#  Scenario: Value of TTL is not null before validation for v1_external#/case-details-endpoint/validateCaseDetailsUsingPOST_1
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Citizen (v1_ext citizen)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_11_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.18
#  Scenario: Value of TTL is not null before validation for v2_external#/case-data-validator-controller/validateUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_MidEvent]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an Mid event callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [validation of a set of fields as Case worker (v2_ext)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_11_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.19
#  Scenario: During Submit case creation adjust any validation on TTL for  v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCaseworker_AboutToSubmit]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [Submit event creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#
#  @S-1029.20
#  Scenario: During Submit case creation adjust any validation on TTL for v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_AboutToSubmit]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [Submit event creation as citizen (v1_ext citizen)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_20_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.21
#  Scenario:  During Submit case creation adjust any validation on TTL for v2_external#/case-controller/createCaseUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_AboutToSubmit]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is still null]
#    And   it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]
#    Then  a positive response is received,
#    And   the response [has the 200 OK code]
#    And   the response has all other details as expected
#    And   the response [contains the TTL set to null]
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_20_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.22
#  Scenario: During Submit case creation About to submit callback updates TTL value for v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCaseworker_AboutToSubmit_NotNull]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [Submit event creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#
#  @S-1029.23
#  Scenario: During Submit case creation About to submit callback updates TTL value for v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_AboutToSubmit_NotNull]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [Submit event creation as citizen (v1_ext citizen)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_23_Remove_Case_Assigned_User_role_for_Case].
#
#  @S-1029.24
#  Scenario: During Submit case creation About to submit callback updates TTL value for v2_external#/case-controller/createCaseUsingPOST
#    Given a user with [an active profile in CCD]
#    And   a successful call [to create a case] as in [F-1029_CreateCasePreRequisiteCitizen_AboutToSubmit_NotNull]
#    When  a request is prepared with appropriate values,
#    And   the request [contains correctly configured event details for case C1]
#    And   the request [contains TTL value is not null]
#    And   the request [is configured to trigger an About to submit callback that has changed the value of TTL and it is not null]
#    And   it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]
#    Then  a negative response is received
#    And   the response [has the 400 OK code]
#    And   the response has all other details as expected
#        # Clean up role assignment made above
#    And a successful call [is made to remove Case Role] as in [F-1029_23_Remove_Case_Assigned_User_role_for_Case].
#
