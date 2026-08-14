@F-1031
Feature: F-1031: Support for the StaffUser field type

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1031.1 #AC-2
    Scenario: must create a case successfully when a StaffUser field is populated
    Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [S-1031_GetCreateToken]

     When a request is prepared with appropriate values
      And the request [contains a StaffUser field populated with an idamId]
      And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/findCaseDetailsForCaseworkerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1031.2 #AC-2
    Scenario: must retrieve a case with its StaffUser value preserved
    Given a user with [an active profile in CCD]
      And a successful call [to create a case containing a StaffUser value] as in [F-1031_CreateCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1031_CreateCasePreRequisiteCaseworker]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]

     Then a positive response is received
      And the response [contains the StaffUser value exactly as it was submitted]
      And the response has all other details as expected
