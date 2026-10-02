#=====================================================
@F-1029.1
Feature: F-1029.1: Validate calls for the display/tab-structure endpoint for new column defaultFocus
#=====================================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

 @S-1029.1
  Scenario: Successful response for case type id with defaultFocus set on tab

    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains valid caseType id which has defaultFocus value set for tab],
    And it is submitted to call the [GET /api/display/tab-structure/{id}] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.


  @S-1029.2
  Scenario: must return case view history when the case reference exists

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
    And a user with [an active profile in CCD],
    And a successful call [to get an event token for just created case] as in [S-164-Prerequisite],
    And another successful call [to update case with the token just created] as in [S-164-Prerequisite_Case_Update],

    When a request is prepared with appropriate values,
    And the request [contains a case that has just been created as in Standard_Full_Case_Creation_Data],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains details of the case just created, along with an HTTP-200 OK],
    And the response [contains the case view history],
    And the response has all other details as expected.
