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
  Scenario: Successfully search for case with the new global search parameters
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004_CreateCasePreRequisiteCaseworker]
    When a request is prepared with appropriate values
    And the request [contains at least one fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response has all other details as expected.
