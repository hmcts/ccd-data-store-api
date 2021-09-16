@F-1004_1
Feature: F-1004: Global Search - Create and update cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1004_1.1
  Scenario:  Update the Data Store for "case create" using V1: /caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-1004_1_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains some data fields to be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004_1.2
  Scenario:  Update the Data Store for "case create" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [F-1004_1_Case_Data_Create_Token_Creation_Citizen]
    When a request is prepared with appropriate values
    And the request [contains some data fields to be used to populate SearchCriteria]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004_1.3
  Scenario:  Update the Data Store for "case create" using V2: /case-types/{caseTypeId}/cases endpoint
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-1004_1_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains some data fields to be used to populate SearchCriteria]
    And it is submitted to call the [Submit Case Creation V2] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1004_1.4
  Scenario:  Update the Data Store for "case update" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004.01_CreateCasePreRequisite]
    And another successful call [to get an event token for the case just created] as in [S-1004_1-GetCaseworkerUpdateToken]
    And another successful call [submit V1 events endpoint] as in [S-1004_1_UpdateCaseEvent]

  @S-1004_1.5
  Scenario:  Update the Data Store for "case update" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004.01_CreateCasePreRequisite]

  @S-1004_1.6
  Scenario:  Update the Data Store for "case update" using V1: /citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1004.01_CreateCasePreRequisite]
