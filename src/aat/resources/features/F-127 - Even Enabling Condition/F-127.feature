#==============================================
@F-127
Feature: F-127: Event Enabling Condition
#==============================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-----------------------------------------------------------------------------------------------------------------------
  @S-127.1 @Ignore
  Scenario: Create Case and check the events when event enabling condition is matching

    Given an appropriate test context as detailed in the test data source
    And a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-127-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_Create_Case_EventEnablingCondition],
    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-127.2 @Ignore
  Scenario: Create Case and check the events when event enabling condition is not matching

    Given an appropriate test context as detailed in the test data source
    And a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-127-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_Create_Case_EventEnablingCondition_NotMatch],
    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
