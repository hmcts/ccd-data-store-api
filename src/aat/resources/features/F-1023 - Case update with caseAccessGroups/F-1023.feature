@F-1023
Feature: F-1023: Submit Case Creation Handle CaseAccessGroups

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


   #=======================================
   # Submit Case Creation Event: v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
   #=======================================

  @Helen
  @S-1023.1 #AC-1
  Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroupId created
    Given     a user with [an active profile in CCD]
    When      a request is prepared with appropriate values
    And       the request [contains correctly configured values]
    And       the request [is of caseType where caseAccessGroupType = CCD:all-cases-access and accessType has a GroupRoleName, -OrganisationPolicyField- CaseAssignedRoleField field exists in caseData and case data has Organisation.OrganisationID value not set to empty value]
    And       it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then      a positive response is received
    #And       in database [case group ID is set in caseAccessGroups collection in the case data and caseGroupType = CCD:all-cases-access]

    @Helen
    @S-1023.2 #AC-2
    Scenario: Invoke saveCaseDetailsForCaseWorkerUsingPOST and caseAccessGroupId not created when caseGroupType != CCD:all-cases-access
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [is of caseType where caseAccessGroupType != CCD:all-cases-access and accessType has a GroupRoleName, -OrganisationPolicyField- CaseAssignedRoleField field exists in caseData and case data has Organisation.OrganisationID value not set to empty value]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      #And     in database [case group ID is not set in caseAccessGroups collection in the case data and caseGroupType != CCD:all-cases-access]

    @Ignore
    @S-1023.3 #AC-3
    Scenario: CaseAccessGroups field contains blank value and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains blank/null value in the CaseAccessGroups field]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response [contains an empty CaseAccessGroups, along with an HTTP-201 OK],
      And     the response has all other details as expected


  @S-1023.4 @Ignore #AC-4
    Scenario: CaseAccessGroups field contains Invalid caseAccessGroupType value and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with Invalid Case Reference]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1023.5 @Ignore #AC-5
    Scenario: CaseAccessGroups field contains valid caseAccessGroupType value but case data invalid and Submit Case Creation Event is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1019_CreateCasePreRequisiteCaseworkerBase]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with valid Case Reference]
      And     the request [contains some invalid case data]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

