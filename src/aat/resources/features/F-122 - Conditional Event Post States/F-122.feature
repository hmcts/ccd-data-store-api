#==============================================
@F-122
Feature: F-122: Conditional Event Post States
#==============================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.1
  Scenario: Defaults the state when none of the post state conditions resolves using AND operator (End state: CaseDeleted)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase2 event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response [contains state: CaseDeleted, updated values for TextField, EmailField along with an HTTP-201 Created],

#-----------------------------------------------------------------------------------------------------------------------

#-----------------------------------------------------------------------------------------------------------------------
