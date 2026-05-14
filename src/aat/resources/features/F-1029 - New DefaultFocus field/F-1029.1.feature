#=====================================================
@F-1029.1
Feature: F-1029.1: Validate calls for the Drafts Endpoint
#=====================================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  @S-1029.1
#  Scenario: Save draft as a caseworker
#
#    Given a user with [an active profile in CCD],
#    And a successful call [to create a token for case creation] as in [F-1029_Get_Event_Token],
#
#    When a request is prepared with appropriate values,
#    And it is submitted to call the [Save draft as a caseworker] operation of [CCD Data Store],
#
#    Then a positive response is received,
#    And the response [contains HTTP 200],
#    And the response has all other details as expected.

#  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  @S-1029.2
#  Scenario: Update draft as a caseworker
#
#    Given a user with [an active profile in CCD],
#    And a successful call [Save draft as a caseworker] as in [F-1029_Create_Draft],
#
#    When a request is prepared with appropriate values,
#    And it is submitted to call the [Update draft as a caseworker] operation of [CCD Data Store],
#
#    Then a positive response is received,
#    And the response [contains HTTP 200],
#    And the response has all other details as expected.
#
#  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-1029.3
  Scenario: Successful response for caseType id and having newly added display_focus column

    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values
    And the request [contains valid caseType id which has display_focus value set for tab]
    When a request is prepared with appropriate values,
    And it is submitted to call the [GET /api/display/display/tab-structure/{id}] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200],
    And the response has all other details as expected.

#  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  @S-1029.6
#  Scenario: Delete a given draft
#
#    Given a user with [an active profile in CCD],
#    And a successful call [Save draft as a caseworker] as in [F-1029_Create_Draft],
#
#    When a request is prepared with appropriate values,
#    And it is submitted to call the [Delete a given draft] operation of [CCD Data Store],
#
#    Then a positive response is received,
#    And the response [contains HTTP 200],
#    And the response has all other details as expected.
#
