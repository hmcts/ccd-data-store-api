@F-111
Feature: F-111: Validate case reference in CaseLink field

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


    @S-111.1
    Scenario: must return validation error when an invalid case reference is entered
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-111_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains an invalid case reference]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-111.2
    Scenario: must return validation error when no case exists for the CaseLink provided
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-111_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains a case reference for a non-existent case]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-111.3
    Scenario: must successfully save CaseLink when a valid reference is entered
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [S-111.3_Create_Case_Data]
      And a successful call [to create a token for case creation] as in [F-111_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains a case reference for an existing case]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected


    @S-111.4 @Ignore
    Scenario: must successfully save CaseLink when a user adds the case reference of a case he/she does not have the required permissions to access
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [S-111.4_Create_Case_Data]
      When a request is prepared with appropriate values
      And the request [contains a case reference for an existing case for which they have no access]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected




