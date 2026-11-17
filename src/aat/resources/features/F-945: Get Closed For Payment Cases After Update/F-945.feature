@F-945
Feature: F-945: Update closed for payment cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a call [to delete existing date case closed records] will get the expected response as in [F-942_DeleteExistingDateCaseClosed],

  @S-945.1
  Scenario: Return updated case when update event post state is closed for payment
    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-944.2_GetToken_CaseCreate_NotClosed],
    And a successful call [to create a case] as in [S-944.2_Create_Case_NotClosed],
    And a successful call [to get an event token for the case just created] as in [S-945.1_GetToken_UpdateCase],
    And a successful call [to submit updateCase event with post state closed for payment] as in [S-945.1_Update_Case_ClosedForPayment],

    When a request is prepared with appropriate values,
    And the request [contains today's Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all other details as expected.

  @S-945.2
  Scenario: Do not return updated case when update event post state is not closed for payment
    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-944.1_GetToken_CaseCreate],
    And a successful call [to create a case] as in [S-944.1_Create_Case],
    And a successful call [to create a token for case creation] as in [S-944.2_GetToken_CaseCreate_NotClosed],
    And a successful call [to create a case] as in [S-944.2_Create_Case_NotClosed],
    And a successful call [to get an event token for the case just created] as in [S-945.2_GetToken_UpdateCase_NotClosedForPayment],
    And a successful call [to submit updateCase event with post state not closed for payment] as in [S-945.2_Update_Case_NotClosedForPayment],

    When a request is prepared with appropriate values,
    And the request [contains today's Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all other details as expected.

  @S-945.3
  Scenario: Do not return updated case after moving out of closed for payment state
    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-944.1_GetToken_CaseCreate],
    And a successful call [to create a case] as in [S-944.1_Create_Case],
    And a successful call [to create a token for case creation] as in [S-944.2_GetToken_CaseCreate_NotClosed],
    And a successful call [to create a case] as in [S-944.2_Create_Case_NotClosed],
    And a successful call [to get an event token for the case just created] as in [S-945.3_GetToken_UpdateCase_ClosedForPayment],
    And a successful call [to submit updateCase event with post state closed for payment] as in [S-945.3_Update_Case_ClosedForPayment],
    And a successful call [to get an event token for the updated case] as in [S-945.3_GetToken_UpdateCase_NotClosedForPayment],
    And a successful call [to submit updateCase event with post state not closed for payment] as in [S-945.3_Update_Case_NotClosedForPayment],

    When a request is prepared with appropriate values,
    And the request [contains today's Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all other details as expected.
