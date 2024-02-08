@F-1022 @crud
Feature: F-1022: Create Case External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1022.1
  Scenario: A new case is created with CaseAccessGroups base type
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-1022_Get_Event_Token_Base]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a positive response is received
    And the response has all other details as expected

  @S-1022.2
  Scenario:  Update a case with CaseAccessGroups base type
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1022_CreateCase]
    And another successful call [to get a caseworker event token to update the case just created] as in [F-1022_GetCaseworkerUpdateToken]
    When a request is prepared with appropriate values
    And the request [contains additional data fields that will be used to populate SearchCriteria]
    And it is submitted to call the [Submit case update event creation as a Caseworker (V1)] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1022.3
  Scenario: Successfully search for case with one of the new global search parameters
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1022_CreateCase]
    When a request is prepared with appropriate values
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has 200 return code],
    And the response has all other details as expected

