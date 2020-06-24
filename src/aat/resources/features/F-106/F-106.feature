@F-106
Feature: F-106: Organisation Policies on Cases

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-900 @Ignore
    Scenario: must successfully create a case with a case type containing some OrganisationPolicy fields
      Given a user with [an active profile in CCD]
      And a successful call [to create a token for case creation] as in [F-106_Case_Data_Create_Token_Creation]
      When a request is prepared with appropriate values
      And the request [contains some OrganisationPolicy fields with all correct values]
      And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
      Then a positive response is received
      And the response has all other details as expected

    @S-901 @Ignore
    Scenario: must successfully update OrganisationPolicy fields on a case

    @S-902 @Ignore
    Scenario: must return e negative response for a case creation attempt with an invalid data in some OrganisationPolicy fields

    @S-902 @Ignore
    Scenario: must return e negative response for a case update attempt with an invalid data in some OrganisationPolicy fields




