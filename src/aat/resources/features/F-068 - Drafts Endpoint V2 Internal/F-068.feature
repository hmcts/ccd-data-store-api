#=====================================================
@F-068
Feature: F-068: Validate calls for the Drafts Endpoint
#=====================================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.1
  Scenario: Save draft as a caseworker

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-068.1_Get_Event_Trigger],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Save draft as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 201 Created],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.2
  Scenario: Update draft as a caseworker

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-068.1_Get_Event_Trigger],
    And a successful call [Save draft as a caseworker] as in [S-068.1],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Update draft as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-068.2
  Scenario: Update draft as a caseworker

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-068.1_Get_Event_Trigger],
    And a successful call [Save draft as a caseworker] as in [S-068.1],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Update draft as a caseworker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

