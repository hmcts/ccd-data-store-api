@F-944
Feature: F-944: Get closed for payment cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a call [to delete existing date case closed records for state category closed for payment] will get the expected response as in [F-944_DeleteExistingDateCaseClosed],
    And a call [to delete existing date case closed records] will get the expected response as in [S-942.1_DeleteExistingDateCaseClosed],

  @S-944.1
  Scenario: Return created case when created case state category is closed for payment
    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-944.1_GetToken_CaseCreate],
    And a successful call [to create a case] as in [S-944.1_Create_Case],

    When a request is prepared with appropriate values,
    And the request [contains today's Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all other details as expected.

  @S-944.2
  Scenario: Do not return created case when created case state category is not closed for payment
    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-944.2_GetToken_CaseCreate_NotClosed],
    And a successful call [to create a case] as in [S-944.2_Create_Case_NotClosed],

    When a request is prepared with appropriate values,
    And the request [contains today's Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [code is HTTP-404],
    And the response has all other details as expected.
