@F-777 @crud
Feature: F-1022: Create Case External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-777.1
  Scenario: A new case is created with CaseAccessGroups base type
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-777_Get_Event_Token_Base]

    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    And the response [contains details of the case just created, along with an HTTP-200 OK],

#    Then a positive response is received
#    And the response has all other details as expected

#
#
#  @S-777.2
#  Scenario: must return case view when the case reference exists
#    Given a user with [an active profile in CCD],
#
#    When a request is prepared with appropriate values,
#    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
#
#    Then a positive response is received,
#    And the response [contains details of the case just created, along with an HTTP-200 OK],
#    And the response has all other details as expected.

#
#  @S-777.3
#  Scenario: Create a case with CaseAccessGroups base type and search case successfully
#
#    Given a user with [an active profile in CCD]
#    And a successful call [to create a token for case creation] as in [F-777_Get_Event_Token_Base]
#
#    When a request is prepared with appropriate values,
#    And it is submitted to call the [external create case] operation of [CCD Data Store],
#
#    Then a positive response is received
#    And the response has all other details as expected









