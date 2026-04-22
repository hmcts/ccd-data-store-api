@F-1026 # Aligning the Professional Access Management capability
Feature: F-1026: Align Professional Access Management Case update with caseAccessGroups

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


   #=======================================
   # Submit Case Creation Event: v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
   #=======================================

  @S-1026.1 #AC-1
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroupId created
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.2 #AC-2 #AC-02 of CCD-5324
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroupId not created when caseGroupType != CCD:all-cases-access
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType is not CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.3 #AC-3 #AC-03 of CCD-5324
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST when OrganisationID is empty
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType = CCD:all-cases-access],
    And       the request [caseData Organisation.OrganisationID value is empty value],
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.5 #Testing Blank value
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroups field contains blank value
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]


   #=======================================
   # Submit Case Creation Event: v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
   #=======================================

  @S-1026.6 #AC-6 #AC-01 of CCD-5324
  Scenario: Invoke saveCaseDetailsForCitizenUsingPOST and caseAccessGroupId created
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.7 #AC-7 #AC-08 of CCD-5324
  Scenario: Invoke saveCaseDetailsForCitizenUsingPOST and caseAccessGroupId not created when caseGroupType != CCD:all-cases-access
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType is not CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.8 #AC-8 #AC-09 of CCD-5324
  Scenario: CaseAccessGroups field contains Invalid caseAccessGroupType value and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType = CCD:all-cases-access],
    And       the request [caseData Organisation.OrganisationID value is empty value],
    And       it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1026.10 #Testing Blank value
  Scenario: Invoke saveCaseDetailsForCitizenUsingPOST and caseAccessGroups field contains blank value
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

   #=======================================
   # Submit Event Creation: v2_external#/case-controller/createCaseUsingPOST
   #=======================================

  @S-1026.11 #AC-11 #AC-04 of CCD-5324
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST and caseAccessGroupId created
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

  @S-1026.12 #AC-12
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST and caseAccessGroupId not created when caseGroupType != CCD:all-cases-access
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType is not CCD:all-cases-access]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

  @S-1026.13 #AC-13
  Scenario: CaseAccessGroups field contains Invalid caseAccessGroupType value and Submit Case Creation Event is invoked on v2_external#/case-controller/createCaseUsingPOST
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where caseAccessGroupType = CCD:all-cases-access],
    And       the request [caseData Organisation.OrganisationID value is empty value],
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

  @S-1026.15 #Testing Blank value
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST and caseAccessGroups field contains blank value
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1026.16 #AC-16  #AC-05 of CCD-5324
  Scenario: Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST and caseAccessGroupId
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1026_CreateCasePreRequisiteCaseworker]
    And   a successful call [to get an event token for the case just created] as in [S-1026-GetUpdateEventToken]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1026_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [contains some OrganisationPolicy fields with all correct values]
    And the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And the request [specifying the case to be updated, as created in F-1026_CreateCasePreRequisiteCaseworker]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1026.17 #AC-17 #AC-06 of CCD-5324
  Scenario: Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST and caseAccessGroupId
    Given a user with [an active profile in CCD]
    And   a successful call [to create a case] as in [F-1026_CreateCasePreRequisiteCitizen]

    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1026_CreateCasePreRequisiteCitizen]
    And the request [contains an event token for the case just created above]
    And the request [contains some OrganisationPolicy fields with all correct values]
    And the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And the request [specifying the case to be updated, as created in F-1026_CreateCasePreRequisiteCitizen]
    And it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v2_external#/case-controller/createEventUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1026.18 #AC-18 #AC-07 of CCD-5324
  Scenario:  Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST and caseAccessGroupId
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1026_CreateCasePreRequisiteCaseworker]

    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1026_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [contains some OrganisationPolicy fields with all correct values]
    And the request [is of caseType where case_data has caseAccessGroupType of CCD:all-cases-access]
    And the request [specifying the case to be updated, as created in F-1026_CreateCasePreRequisiteCaseworker]
    And it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected



