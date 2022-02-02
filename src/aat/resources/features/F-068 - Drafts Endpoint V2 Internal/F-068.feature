#=====================================================
@F-068
Feature: F-068: Validate calls for the Drafts Endpoint
#=====================================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.1
  Scenario: Save draft as a caseworker

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [F-068_Get_Event_Token],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Save draft as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 201 Created],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.2
  Scenario: Update draft as a caseworker

    Given a user with [an active profile in CCD],
    And a successful call [Save draft as a caseworker] as in [F-068_Create_Draft],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Update draft as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.3
  Scenario: Fetch a draft for display

    Given a user with [an active profile in CCD],
    And a successful call [Save draft as a caseworker] as in [F-068_Create_Draft],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Fetch a draft for display] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.4
  Scenario: Delete a given draft

    Given a user with [an active profile in CCD],
    And a successful call [Save draft as a caseworker] as in [F-068_Create_Draft],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Delete a given draft] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

