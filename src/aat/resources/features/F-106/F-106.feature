@F-106
Feature: F-106: Submit Case Creation With Organisation Policy

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-900
    Scenario: must return 200 for case creation with organisation policy
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-106_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And it is submitted to call the [submit case creation as caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected


    @S-901 @Ignore
    Scenario: must return 200 for case update with organisation policy

    @S-902 @Ignore
    Scenario: must return 40X for case creation with incorrect organisation policy




