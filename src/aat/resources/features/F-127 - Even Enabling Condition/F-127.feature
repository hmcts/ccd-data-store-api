#==============================================
@F-127
Feature: F-122: Event Enabling Condition
#==============================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-----------------------------------------------------------------------------------------------------------------------
  @S-127.1
  Scenario: Create Case and check the events when event enabling condition is not valid

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-127-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.2
  Scenario: Create Case and check the events when event enabling condition is valid

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-127-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case_Event_NotMatch],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
