@F-151 @CCD-4657
Feature: F-151: Get case data for a given case type as a caseworker

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-151.1
  Scenario: must return the newly created case when searching by case reference
    Given a user with [an active caseworker profile in CCD],
      And a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation],
      And a successful call [to create a full case] as in [F-151_CreateCase],

    When a request is prepared with appropriate values,
      And the request [contains the reference of the newly created case],
      And it is submitted to call the [get case data for a given case type as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
      And the response [contains only the newly created case and its data, along with an HTTP 200 OK],
      And the response has all other details as expected.
