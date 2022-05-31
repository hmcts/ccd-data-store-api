@F-1018
Feature: F-1018: Submit Event Creation Handle Case Links

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


   #=======================================
   # Submit Event Creation: v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
   #=======================================

    @S-1018.1 #AC-1
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.1-VerifyCaseLinks]

    @S-1018.2 #AC-2
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, CaseLink field changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.2-VerifyCaseLinks]

    @S-1018.3 #AC-3
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyRemovedCaseLinks]

    @S-1018.4 #AC-4
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.5 #AC-5
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field set to blank]
      And the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.6 #AC-6
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.7 #AC-7
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.8 #AC-8
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.9 #AC-9
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.10 #AC-10
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.11 #AC-11
    Scenario: Case Link does not exist at present and CaseLink field in the Request is a collection and contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyMultipleCaseLinks]

    @S-1018.12 #AC-12
    Scenario: Case Link value changed and CaseLink field in the Request is a collection and contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.12-VerifyCaseLinks]

    @S-1018.13 #AC-13
    Scenario: CaseLink in database exists but CaseLink field in the Request is a collection and contains blank/null value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.13-VerifyCaseLinks]

    @S-1018.14 #AC-14
    Scenario: CaseLink in database exists and CaseLink field in the Request is a collection and is unchanged and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1018.15 #AC-15
    Scenario: CaseLink in database Does NOT exist and CaseLink field is a collection in the Request and is blank and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.16 #AC-16
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.17 #AC-17
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1018.18 #AC-18
    Scenario: CaseLink in database exists but CaseLink field in the Request is a collection and contains blank/null value, some invalid case data is submitted and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1018.19 #AC-19
    Scenario: Case Link does not exist at present and CaseLink field in the Request is a collection and contains CaseReference value but Invalid Case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.20 #AC-20
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case with a different case_type] as in [F-1018_CreateThirdCaseDifferentCaseTypePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyMultipleLinkedCaseLinksUnchanged]


   #=======================================
   # Submit Event Creation: v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
   #=======================================

    @S-1018.21 #AC-21
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.21-VerifyCaseLinks]

    @S-1018.22 #AC-22
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateLinkedCasePreRequisiteCitizenBase, CaseLink field changed]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.22-VerifyCaseLinks]

    @S-1018.23 #AC-23
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyRemovedCitizenCaseLinks]

    @S-1018.24 #AC-24
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateLinkedCasePreRequisiteCitizenBase, CaseLink field not changed]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1018.25 #AC-25
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field set to blank]
      And the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCitizenCaseLinks]

    @S-1018.26 #AC-26
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCitizenCaseLinks]

    @S-1018.27 #AC-27
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1018.28 #AC-28
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1018.29 #AC-29
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCitizenBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCitizenCaseLinks]

    @S-1018.30 #AC-30
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCitizenBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018_VerifyCitizenLinkedCaseLinksUnchanged]


   #=======================================
   # Submit Event Creation: v2_external#/case-controller/createEventUsingPOST
   #=======================================

    @S-1018.31 #AC-31
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.1-VerifyCaseLinks]

    @S-1018.32 #AC-32
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase, CaseLink field changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1018.2-VerifyCaseLinks]

    @S-1018.33 #AC-33
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyRemovedCaseLinks]

    @S-1018.34 #AC-34
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.35 #AC-35
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field set to blank]
      And the request [specifying the case to be updated, as created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.36 #AC-36
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.37 #AC-37
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.38 #AC-38
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]

    @S-1018.39 #AC-39
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1018_VerifyBlankCaseLinks]

    @S-1018.40 #AC-40
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1018_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1018_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links in the CASE_LINK table are unchanged] as in [F-1018_VerifyLinkedCaseLinksUnchanged]


   #=======================================
   # Submit Event Creation: extra tests for Standard CaseLinks field and flag in CaseLinks table
   #=======================================

    @S-1018.41
    Scenario: Standard CaseLinks field should generate caseLink records with StandardLink set to true when Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case with many case links] as in [F-1018_CreateCaseWithStandardCaseLinksByCaseworker]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken_ForStandardCaseLinkTests]
      When  a request is prepared with appropriate values
      And   the request [contains the standard CaseLinks field with Case Reference values]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1018_VerifyMultipleCaseLinksUsingStandardLinkField_Caseworker]

    @S-1018.42
    Scenario: Standard CaseLinks field should generate caseLink records with StandardLink set to true when Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case with many case links] as in [F-1018_CreateCaseWithStandardCaseLinksByCitizen]
      And   a successful call [to get an update event token for the case just created as a Citizen] as in [F-1018-GetCitizenUpdateEventToken_ForStandardCaseLinkTests]
      When  a request is prepared with appropriate values
      And   the request [contains the standard CaseLinks field with Case Reference values]
      And   it is submitted to call the [submit event creation as citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1018_VerifyMultipleCaseLinksUsingStandardLinkField_Citizen]

    @S-1018.43
    Scenario: Standard CaseLinks field should generate caseLink records with StandardLink set to true when Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case with many case links] as in [F-1018_CreateCaseWithStandardCaseLinksByCaseworker]
      And   a successful call [to get an event token for the case just created] as in [F-1018-GetUpdateEventToken_ForStandardCaseLinkTests]
      When  a request is prepared with appropriate values
      And   the request [contains the standard CaseLinks field with Case Reference values]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Links have been created in the CASE_LINK table with correct values] as in [F-1018_VerifyMultipleCaseLinksUsingStandardLinkField_Caseworker]
