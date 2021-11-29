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
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.1-VerifyCaseLinks]

    @Ignore
    @S-1008.2 @AC-2
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, CaseLink field changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.2-VerifyCaseLinks]

    @Ignore
    @S-1008.3 @AC-3
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.4 @AC-4
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @S-1008.5 @AC-5
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @S-1008.6 @AC-6
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.7 @AC-7
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @Ignore
    @S-1008.8 @AC-8
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @S-1008.9 @AC-9
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.10 @AC-10
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @S-1008.11 @AC-11
    Scenario: Case Link does not exist at present and CaseLink field in the Request is a collection and contains CaseReference value
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyMultipleCaseLinks]

    @Ignore
    @S-1008.12 @AC-12
    Scenario: Case Link value changed and CaseLink field in the Request is a collection and contains CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.12-VerifyCaseLinks]

    @Ignore
    @S-1008.13 @AC-13
    Scenario: CaseLink in database exists but CaseLink field in the Request is a collection and contains blank/null value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.13-VerifyCaseLinks]

    @Ignore
    @S-1008.14 @AC-14
    Scenario: CaseLink in database exists and CaseLink field in the Request is a collection and is unchanged and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1008.15 @AC-15
    Scenario: CaseLink in database Does NOT exist and CaseLink field is a collection in the Request and is blank and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @S-1008.16 @AC-16
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.17 @AC-17
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyMultipleLinkedCaseLinksUnchanged]

    @Ignore
    @S-1008.18 @AC-18
    Scenario: CaseLink in database exists but CaseLink field in the Request is a collection and contains blank/null value, some invalid case data is submitted and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1008.19 @AC-19
    Scenario: Case Link does not exist at present and CaseLink field in the Request is a collection and contains CaseReference value but Invalid Case data and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.20 @AC-20
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateThirdCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateMultipleLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetMultipleLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field as a collection]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, has a different CaseLink field in CaseCollection]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyMultipleLinkedCaseLinksUnchanged]

    @S-1008.21 @AC-21
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, does not contain a CaseLink field]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.21-VerifyCaseLinks]

    @Ignore
    @S-1008.22 @AC-22
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, CaseLink field changed]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.22-VerifyCaseLinks]

    @Ignore
    @S-1008.23 @AC-23
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCitizenCaseLinks]

    @Ignore
    @S-1008.24 @AC-24
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCitizenBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCitizenBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1008.25 @AC-25
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and
              Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCitizenCaseLinks]

    @S-1008.26 @AC-26
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCitizenCaseLinks]

    @Ignore
    @S-1008.27 @AC-27
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyCitizenLinkedCaseLinksUnchanged]

    @Ignore
    @S-1008.28 @AC-28
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1008.29 @AC-29
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetCitizenUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCitizenCaseLinks]

    @Ignore
    @S-1008.30 @AC-30
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data
              and Submit Event Creation is invoked on v1_external#/case-details-endpoint/createCaseEventForCitizenUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCitizenBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCitizenBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCitizenCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Citizen] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyCitizenLinkedCaseLinksUnchanged]

    @S-1008.31 @AC-31
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
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
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.1-VerifyCaseLinks]

    @S-1008.32 @AC-32
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value and
              Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase, CaseLink field changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008.2-VerifyCaseLinks]

    @Ignore
    @S-1008.33 @AC-33
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value and
              Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [does not specify a case to be updated]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.34 @AC-34
    Scenario: CaseLink in database exists and CaseLink field in the Request is unchanged and
              Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @S-1008.35 @AC-35
    Scenario: CaseLink in database Does NOT exist and CaseLink field in the Request is blank and
              Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [specifying the case to be updated, as created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase, CaseLink field not changed]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a positive response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @S-1008.36 @AC-36
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.37 @AC-37
    Scenario: Case Link value changed and CaseLink field in the Request contains INVALID CaseReference value
              and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has an invalid reference]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @Ignore
    @S-1008.38 @AC-38
    Scenario: CaseLink in database exists but CaseLink field in the Request contains blank/null value, some invalid case data is submitted
              and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a blank reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]

    @S-1008.39 @AC-39
    Scenario: Case Link does not exist at present and CaseLink field in the Request contains CaseReference value but Invalid Case data
              and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that no Case Links exist in the CASE_LINK table] as in [F-1008_VerifyBlankCaseLinks]

    @Ignore
    @S-1008.40 @AC-40
    Scenario: Case Link value changed and CaseLink field in the Request contains CaseReference value, but Invalid case data
              and Submit Event Creation is invoked on v2_external#/case-controller/createEventUsingPOST
    Given   a user with [an active profile in CCD]
      And   a successful call [to create a case] as in [F-1008_CreateCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateLinkedCasePreRequisiteCaseworkerBase]
      And   another successful call [to create a case] as in [F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   a successful call [to get an event token for the case just created] as in [F-1008-GetLinkedCaseUpdateEventToken]
      When  a request is prepared with appropriate values
      And   the request [contains correctly configured CaseLink field with Case Reference created in F-1008_CreateAnotherCasePreRequisiteCaseworkerBase]
      And   the request [CaseLink field has a valid reference]
      And   the request [Case data is invalid]
      And   it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
      Then  a negative response is received
      And   the response has all other details as expected
      And   a successful call [to verify that the Case Link has been created in the CASE_LINK table with correct value] as in [F-1008_VerifyLinkedCaseLinksUnchanged]
