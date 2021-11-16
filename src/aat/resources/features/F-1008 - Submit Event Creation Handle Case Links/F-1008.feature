@F-1008
Feature: F-1008: Submit Event Creation Handle Case Links

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-1008.1 @AC-1
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008-VerifyCaseLink]

#      And the request [contains a case Id that has just been created as in F-1007_CreateSuspendedCasePreRequisiteCaseworker]
#      And the request [contains an event token for the case just created above]
#      And the request [has the mid event callback change the TTL.Suspended value changed]
#      And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
#     Then a negative response is received
#      And the response has all other details as expected

