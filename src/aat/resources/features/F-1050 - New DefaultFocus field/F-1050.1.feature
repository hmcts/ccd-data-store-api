#=====================================================
@F-1050.1
Feature: F-1050.1: Validate calls for the display/tab-structure endpoint for new column defaultFocus
#=====================================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

 @S-1050.1
  Scenario: Successful response for case type id with defaultFocus set on tab

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [S-1050.1_Create_Case_Data],
    When a request is prepared with appropriate values,
    And the request [contains valid caseType id which has defaultFocus value set for tab],
    And it is submitted to call the [fetch a case for display for Case Worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.


  @S-1050.2
  Scenario: Successful response for case type id with tabs that do not have defaultFocus set

    Given a user with [an active profile in CCD],
    And a successful call [to start decentralised case creation] as in [F-150_CreateCase_Token_Creation],
    And a successful call [to create a decentralised case] as in [S-150.01_Prereq],

    When a request is prepared with appropriate values,
    And the request [contains valid caseType id FT_Decentralisation whose tab has no defaultFocus value],
    And it is submitted to call the [fetch a case for display for Case Worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.
