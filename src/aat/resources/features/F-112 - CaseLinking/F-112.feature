@F-112
Feature: F-112: Validate case reference in CaseLink field

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


    @S-112.1
    Scenario: must return validation error when an invalid case reference is entered
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-112_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains an invalid case reference]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-112.2
    Scenario: must return validation error when no case exists for the CaseLink provided
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-112_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains a case reference for a non-existent case]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-112.3
    Scenario: must successfully save CaseLink when a valid reference is entered
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [S-112.3_Create_Case_Data]
      And a successful call [to create a token for case creation] as in [F-112_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains a case reference for an existing case to which the user has access]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected


    @S-112.4
    Scenario: must successfully save CaseLink when a user adds the case reference of a case he/she does not have the required permissions to access
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a successful call [to create a token for case creation] as in [F-112_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains a case reference for an existing case to which the user has no access]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected




