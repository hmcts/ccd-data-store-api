@F-1004
Feature: F-1004: Global Search - Create and update cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1004.1
  Scenario:  Update the Data Store for "case create" using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.2
  Scenario:  Update the Data Store for "case create" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [F-1004_Case_Data_Create_Token_Creation_Citizen]
    When a request is prepared with appropriate values
    And the request [contains data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.3
  Scenario:  Update the Data Store for "case create" using V2: /case-types/{caseTypeId}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.4
  Scenario:  Update the Data Store for "case update" using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.5
  Scenario:  Update the Data Store for "case update" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case as a citizen] as in [F-1004_CreateCasePreRequisiteCitizen]
    And another successful call [to get a citizen event token to update the case just created] as in [F-1004_GetCitizenUpdateToken]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Citizen (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.6
  Scenario:  Update the Data Store for "case update" using V2: /cases/{caseId}/events endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.7
  Scenario:  Successfully creates a case with wrong data type for Date fields using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.8
  Scenario:  Successfully creates a case with wrong data type for Date fields using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [F-1004_Case_Data_Create_Token_Creation_Citizen]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.9
  Scenario:  "Successfully creates a case with wrong data type for Date fields using V2: /case-types/{caseTypeId}/cases endpoint",
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.10
  Scenario:  Successfully updates a case with wrong data type for Date fields using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_InvalidDataFields]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_InvalidFieldsCase]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.11
  Scenario:  Successfully updates a case with wrong data type for Date fields using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case as a citizen] as in [F-1004_CreateCasePreRequisiteCitizen_InvalidDateFields]
    And another successful call [to get a citizen event token to update the case just created] as in [F-1004_GetCitizenUpdateToken_InvalidFieldsCase]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case update event creation as a Citizen (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.12
  Scenario:  Successfully updates a case with wrong data type for Date fields using V2: /cases/{caseId}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_InvalidDataFields]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_InvalidFieldsCase]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.13
  Scenario:  Successfully creates a case with correct data type for Date fields using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty including the Dates]
    And the response has all other details as expected

  @S-1004.14
  Scenario:  Successfully updates a case with correct data type for Date fields using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_InvalidDataFields]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_InvalidFieldsCase]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty including the Dates]
    And the response has all other details as expected

  @S-1004.15
  Scenario:  Update the Data Store for "case create" when valid data has been entered correctly in the CollectionFieldName using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains the collection field that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.16
  Scenario:  Update the Data Store for "case create" when valid data has been entered correctly in the CollectionFieldName using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [F-1004_Case_Data_Create_Token_Creation_Citizen]
    When a request is prepared with appropriate values
    And the request [contains the collection field that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.17
  Scenario:  Update the Data Store for "case create" when valid data has been entered correctly in the CollectionFieldName using V2: /case-types/{caseTypeId}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains the collection field that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004.18
  Scenario:  Update the Data Store for "case update" when valid data has been entered correctly in the CollectionFieldName using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_Collection]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_Collection]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V1)] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [contains updated data including the search party fields in the collection fields as specified by the CollectionFieldName],
    And the response has all other details as expected

  @S-1004.19
  Scenario:  Update the Data Store for "case update" when valid data has been entered correctly in the CollectionFieldName using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case as a citizen] as in [F-1004_CreateCasePreRequisiteCitizen_Collection]
    And another successful call [to get a citizen event token to update the case just created] as in [F-1004_GetCitizenUpdateToken_Collection]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Citizen (V1)] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [contains updated data including the search party fields in the collection fields as specified by the CollectionFieldName],
    And the response has all other details as expected

  @S-1004.20
  Scenario:  Update the Data Store for "case update" when valid data has been entered correctly in the CollectionFieldName using V2: /cases/{caseId}/events endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_Collection]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_Collection]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V2)] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [contains updated data including the search party fields in the collection fields as specified by the CollectionFieldName],
    And the response has all other details as expected

  @S-1004.21
  Scenario:  Create and update a case using a collection field within a complex type in the CollectionFieldName column
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_ComplexCollection]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_ComplexCollection]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V2)] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [contains updated data including the search party fields in the collection fields as specified by the CollectionFieldName],
    And the response has all other details as expected

  @S-1004.22
  Scenario:  Successfully creates a case with wrong data type for Date fields in a collection
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a caseworker] as in [F-1004_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields in a collection]
    And it is submitted to call the [Submit case creation as Case worker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected

  @S-1004.23
  Scenario:  Successfully updates a case with wrong data type for Date fields in a collection
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker_InvalidDataFieldsCollection]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1004_GetCaseworkerUpdateToken_InvalidFieldsCaseCollection]
    When a request is prepared with appropriate values
    And the request [contains a Text value for the Date fields in a collection]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a SearchCriteria with SearchParty excluding the Dates]
    And the response has all other details as expected
