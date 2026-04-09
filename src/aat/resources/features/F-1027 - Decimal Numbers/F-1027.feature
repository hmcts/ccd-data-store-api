@F-1027
Feature: F-1027: Validate numberField accepts and displays decimals

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1027.3
  Scenario: must successfully create case with number field displaying as expected
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [S-1027.3_Create_Case_Data]
    And a successful call [to create a token for case creation] as in [F-1027_Case_Data_Create_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains a case with number field showing correctly]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected


