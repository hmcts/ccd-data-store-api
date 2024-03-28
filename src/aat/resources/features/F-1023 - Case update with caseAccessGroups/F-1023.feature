@F-1023
Feature: F-1023: Submit Case Creation Handle CaseAccessGroups

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


   #=======================================
   # Submit Case Creation Event: v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
   #=======================================

  @S-1023.1 #AC-1
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

  @S-1023.2 #AC-2
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

  @S-1023.3 #AC-3
  Scenario: CaseAccessGroups field contains Invalid caseAccessGroupType value and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
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

  @S-1023.4 #AC-4
  Scenario: CaseAccessGroups field contains valid caseAccessGroupType value but case data invalid and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [is of caseType where caseAccessGroupType = CCD:all-cases-access and -OrganisationPolicyField- CaseAssignedRoleField  field exists in caseData and case data has Organisation.OrganisationID value not set to empty value but accessType does not have GroupRoleName]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]

  @S-1023.5 #Testing Blank value
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroups field contains blank value
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains some OrganisationPolicy fields with all correct values]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    And       the response has all other details as expected
    And       the response [contains updated values for case_data and data_classification]
