@F-1025
Feature: F-1025: Submit Case Creation Handle OrganisationProfileField with newCase

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


   #=======================================
   # Submit Case Creation Event: v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
   #=======================================

  @S-1025.1 #AC-1
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and organisationProfile with no newCase
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has organisationProfile with no newCase]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1025.2 #AC-2
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST with newCase set to No
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has organisationProfile with newCase set to No]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1025.3 #AC-3
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST when OrganisationID is empty
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has organisationProfile with newCase set to Yes],
    And       the request [caseData Organisation.OrganisationID value is empty value],
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1025.4 #AC-4
  Scenario: case_data has organisationProfile with newCase set to YES and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [is of caseType where case_data case data has Organisation.OrganisationID and organisationProfile with newCase set to Yes]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1025.5 #AC-5
  Scenario: case_data has multiple organisationProfile with newCase set to YES or No and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [is of caseType where case_data case data has Organisation.OrganisationID and multiple organisationProfile with newCase set to Yes or No]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

   #=======================================
   # Submit Event Creation: v2_external#/case-controller/createCaseUsingPOST
   #=======================================

  @S-1025.6 #AC-06
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST has organisationProfile with newCase set to No
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has organisationProfile with newCase set to No]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

  @S-1025.7 #AC-07
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST and has organisationProfile with newCase set to Yes
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has organisationProfile with newCase set to YES]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]

  @S-1025.8 #AC-08
  Scenario: Invoke v2_external#/case-controller/createCaseUsingPOST and has multiple organisationProfile with newCase set to Yes/No
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       the request [is of caseType where case_data has multiple organisationProfile with newCase set to YES/No]
    And       it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for data and data_classification]


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1025.9 #AC-09
  Scenario: Submit Event is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST and case_data has organisationProfile with newCase set to YES
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1025_CreateCasePreRequisiteCaseworker]
    And   a successful call [to get an event token for the case just created] as in [S-1025-GetUpdateEventToken]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1025_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [contains some OrganisationPolicy fields with all correct values]
    And the request [is of caseType where case_data case data has Organisation.OrganisationID and organisationProfile with newCase set to Yes]
    And the request [specifying the case to be updated, as created in F-1025_CreateCasePreRequisiteCaseworker]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v2_external#/case-controller/createEventUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1025.10 #AC-10
  Scenario:  Submit Event is invoked on v2_external#/case-controller/createEventUsingPOST and multiple organisationProfile with newCase set to Yes or No
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1025_CreateCasePreRequisiteCaseworker]

    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in F-1025_CreateCasePreRequisiteCaseworker]
    And the request [contains an event token for the case just created above]
    And the request [contains some OrganisationPolicy fields with all correct values]
    And the request [is of caseType where case data has Organisation.OrganisationID and multiple organisationProfile with newCase set to Yes or No]
    And the request [specifying the case to be updated, as created in F-1025_CreateCasePreRequisiteCaseworker]
    And it is submitted to call the [Submit event creation (v2_ext)] operation of [CCD Data Store]

    Then  a positive response is received
    And   the response has all other details as expected



