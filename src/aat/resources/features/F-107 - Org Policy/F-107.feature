@F-107
Feature: F-107: Organisation Policies on Cases

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-929
    Scenario: must successfully create a case with a case type containing some OrganisationPolicy fields
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-107_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains some OrganisationPolicy fields with all correct values]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected

    @S-930
    Scenario: must successfully update OrganisationPolicy fields on a case
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [S-930_Create_Case_Data]
      And a successful call [to get an event token for the case just created] as in [S-930-Prerequisite]
      When a request is prepared with appropriate values
      And the request [contains some OrganisationPolicy fields with all correct values]
      And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
      Then a positive response is received
      And the response has all other details as expected

    @S-931
    Scenario: must return e negative response for a case creation attempt with an invalid data in some OrganisationPolicy fields
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-107_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains OrganisationPolicy case roles which are different from the default case roles]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a negative response is received
      And the response has all other details as expected

    @S-928
    Scenario: must return e negative response for a case update attempt with an invalid data in some OrganisationPolicy fields
      Given a user with [an active profile in CCD]
      And a case that has just been created as in [S-930_Create_Case_Data]
      And a successful call [to get an event token for the case just created] as in [S-930-Prerequisite]
      When a request is prepared with appropriate values
      And the request [contains OrganisationPolicy case roles which are different from the default case roles]
      And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
      Then a negative response is received
      And the response has all other details as expected




