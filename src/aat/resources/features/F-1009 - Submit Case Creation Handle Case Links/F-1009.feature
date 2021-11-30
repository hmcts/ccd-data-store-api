@F-1009
Feature: F-1009: Submit Case Creation Handle Case Links

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-1009.1 @AC-1
    Scenario: CaseLink field contains CaseReference value and Submit Case Creation Event is invoked
              on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1009_CreateCasePreRequisiteCaseworkerBase]
      And   it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1009-VerifyCaseLinks]

    @S-1009.2 @AC-2
    Scenario: CaseLink field contains blank value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains blank/null value in the CaseLink field]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that no Case Links have been created in the CASE_LINK table] as in [F-1009-VerifyCaseLinksNotInserted]

    @S-1009.3 @AC-3
    Scenario: CaseLink field contains Invalid CaseReference value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with Invalid Case Reference]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.4 @AC-4
    Scenario: CaseLink field contains valid CaseReference value but case data invalid and Submit Case Creation Event
              is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with valid Case Reference]
      And     the request [contains some invalid case data]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.5 @AC-5
    Scenario: Collection of CaseLink fields contains CaseReference value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      And     a successful call [to create a case] as in [F-1009_CreateAnotherCasePreRequisiteCaseworkerBase]
      When    a request is prepared with appropriate values
      And     the request [contains collection of correctly configured CaseLink collection field with Case Reference values]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1009-VerifyMultipleCaseLinks]

    @S-1009.6 @AC-6
    Scenario: CaseLink field contains CaseReference value and Submit Case Creation Event is invoked
              on v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCitizenBase]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1009_CreateCasePreRequisiteCitizenBase]
      And   it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1009-VerifyCaseLinks]

    @S-1009.7 @AC-7
    Scenario: CaseLink field contains blank value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains blank/null value in the CaseLink field]
      And     it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that no Case Links have been created in the CASE_LINK table] as in [F-1009-VerifyCaseLinksNotInserted]

    @S-1009.8 @AC-8
    Scenario: CaseLink field contains Invalid CaseReference value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with Invalid Case Reference]
      And     it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.9 @AC-9
    Scenario: CaseLink field contains valid CaseReference value but case data invalid and Submit Case Creation Event
              is invoked on v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCitizenBase]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with valid Case Reference]
      And     the request [contains some invalid case data]
      And     it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.10 @AC-10
    Scenario: Collection of CaseLink fields contains CaseReference value and Submit Case Creation Event is invoked on
              v1_external#/case-details-endpoint/saveCaseDetailsForCitizenUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCitizenBase]
      And     a successful call [to create a case] as in [F-1009_CreateAnotherCasePreRequisiteCitizenBase]
      When    a request is prepared with appropriate values
      And     the request [contains collection of correctly configured CaseLink collection field with Case Reference values]
      And     it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1009-VerifyMultipleCaseLinks]

    @S-1009.11 @AC-11
    Scenario: CaseLink field contains CaseReference value and Submit Case Creation Event is invoked
              on v2_external#/case-controller/createCaseUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1009_CreateCasePreRequisiteCaseworkerBase]
      And   it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1009-VerifyCaseLinks]

    @S-1009.12 @AC-12
    Scenario: CaseLink field contains blank value and Submit Case Creation Event is invoked on
              v2_external#/case-controller/createCaseUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains blank/null value in the CaseLink field]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that no Case Links have been created in the CASE_LINK table] as in [F-1009-VerifyCaseLinksNotInserted]

    @S-1009.13 @AC-13
    Scenario: CaseLink field contains Invalid CaseReference value and Submit Case Creation Event is invoked on
              v2_external#/case-controller/createCaseUsingPOST
      Given   a user with [an active profile in CCD]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with Invalid Case Reference]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.14 @AC-14
    Scenario: CaseLink field contains valid CaseReference value but case data invalid and Submit Case Creation Event
              is invoked on v2_external#/case-controller/createCaseUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      When    a request is prepared with appropriate values
      And     the request [contains correctly configured CaseLink field with valid Case Reference]
      And     the request [contains some invalid case data]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a negative response is received,
      And     the response [has the 422 return code],
      And     the response has all other details as expected.

    @S-1009.15 @AC-15
    Scenario: Collection of CaseLink fields contains CaseReference value and Submit Case Creation Event is invoked on
              v2_external#/case-controller/createCaseUsingPOST
      Given   a user with [an active profile in CCD]
      And     a successful call [to create a case] as in [F-1009_CreateCasePreRequisiteCaseworkerBase]
      And     a successful call [to create a case] as in [F-1009_CreateAnotherCasePreRequisiteCaseworkerBase]
      When    a request is prepared with appropriate values
      And     the request [contains collection of correctly configured CaseLink collection field with Case Reference values]
      And     it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
      Then    a positive response is received
      And     the response has all other details as expected
      And     a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1009-VerifyMultipleCaseLinks]
