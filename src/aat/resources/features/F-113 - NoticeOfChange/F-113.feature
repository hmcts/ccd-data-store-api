@F-113
Feature: F-113: ChangeOrganisationRequest Complex Base Type

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-COR
  Scenario: must successfully create a case with a case type containing ChangeOrganisationRequest Complex Fields
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-113_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains some ChangeOrganisationRequest Complex Fields with all correct values]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-113.2
  Scenario: must return negative response for a case creation attempt with an invalid data in ChangeOrganisationRequest Complex Fields
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-113_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains ChangeOrganisationRequest complex with invalid approval status]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected



