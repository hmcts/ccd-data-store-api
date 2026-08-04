@F-1029
Feature: F-1029: Support for the RichTextArea field type

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/saveCaseDetailsForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1029.1 #AC-2
    Scenario: must create a case successfully when a RichTextArea field is populated with rich text mark-up
    Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [S-1029_GetCreateToken]

     When a request is prepared with appropriate values
      And the request [contains a RichTextArea field populated with rich text mark-up]
      And it is submitted to call the [Submit Case Creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

  @S-1029.2 #AC-2
    Scenario: must get a negative response when a RichTextArea field is populated with a value shorter than the configured minimum
    Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [S-1029_GetCreateToken]

     When a request is prepared with appropriate values
      And the request [contains a RichTextArea field with a Min of 10 configured against it]
      And the request [contains a value for that field which is shorter than the configured minimum]
      And it is submitted to call the [Submit Case Creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

  @S-1029.5 #AC-2
    Scenario: must get a negative response when a non-string value is submitted against a RichTextArea field
    Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [S-1029_GetCreateToken]

     When a request is prepared with appropriate values
      And the request [contains a number where the RichTextArea field expects a string]
      And it is submitted to call the [Submit Case Creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a negative response is received
      And the response has all other details as expected

  @S-1029.6 #AC-2
    Scenario: must create a case successfully when an optional RichTextArea field is null
    Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [S-1029_GetCreateToken]

     When a request is prepared with appropriate values
      And the request [contains a null value for the RichTextArea field]
      And it is submitted to call the [Submit Case Creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/createCaseEventForCaseWorkerUsingPOST
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1029.3 #AC-3
    Scenario: must update a RichTextArea field successfully through an event submission
    Given a user with [an active profile in CCD]
      And a successful call [to create a case containing rich text mark-up] as in [F-1029_CreateCasePreRequisiteCaseworker]
      And a successful call [to get an event token for the case just created] as in [S-1029_GetUpdateToken]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1029_CreateCasePreRequisiteCaseworker]
      And the request [contains an event token for the case just created above]
      And the request [contains a new value for the RichTextArea field]
      And it is submitted to call the [Submit event creation as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# v1_external#/case-details-endpoint/findCaseDetailsForCaseworkerUsingGET
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1029.4 #AC-4
    Scenario: must retrieve a RichTextArea field with its mark-up preserved
    Given a user with [an active profile in CCD]
      And a successful call [to create a case containing rich text mark-up] as in [F-1029_CreateCasePreRequisiteCaseworker]

     When a request is prepared with appropriate values
      And the request [contains a case Id that has just been created as in F-1029_CreateCasePreRequisiteCaseworker]
      And it is submitted to call the [Get case details as Case worker (v1_ext caseworker)] operation of [CCD Data Store]

     Then a positive response is received
      And the response [contains the RichTextArea value exactly as it was submitted]
      And the response has all other details as expected
