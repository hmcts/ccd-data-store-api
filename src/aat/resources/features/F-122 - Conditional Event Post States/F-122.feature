#==============================================
@F-122
Feature: F-122: Conditional Event Post States
#==============================================
#  Tests 1-3 use updateCase event with the post state condition:
#  CaseCreated -> CaseUpdated(TextField="updated" AND EmailField="*"):1;CaseAmended(TextField="amended" AND EmailField="*"):2;CaseRevoked(AddressField.AddressLine1="revoked" AND EmailField="*"):3;CaseDeleted
#
#  Tests 4-7 use updateCase2 event with the post state condition:
#  CaseCreated -> CaseUpdated2(TextField="updated2" OR EmailField="*"):1;CaseAmended2(TextField="amended2" OR EmailField="matched@test.com"):2;*(TextField="keepstate"):3;CaseRevoked2(EmailField!=""):4;CaseDeleted

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
    And it is submitted to call the [submit updateCase event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected
    And the response [contains state: CaseDeleted, updated values for TextField, EmailField along with an HTTP-201 Created],


#-----------------------------------------------------------------------------------------------------------------------
  @S-122.2
  Scenario: Sets the state defined with a matching post state condition using AND operator (End state: CaseAmended)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.3
  Scenario: EmailField="*" operator should match when field is not defined (End state: CaseAmended)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.4
  Scenario: Sets the state defined with a matching post state condition using a complex field (End state: CaseRevoked)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase event with AddressField.AddressLine1 and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.5
  Scenario: Ordering should take precedence when both conditions resolve (End state: CaseAmended2)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase2 event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.6
  Scenario: Sets the state defined with a matching post state condition using OR operator (End state: CaseUpdated2)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase2 event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.7
  Scenario: *(FieldA) will keep the state as is (End state: CaseCreated)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase2 event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------
  @S-122.8
  Scenario: Sets the state defined with a matching post state condition using != operator (End state: CaseRevoked2)

    Given a user with [an active profile in CCD],
    And a successful call [to create a token for case creation] as in [S-122-GetToken_CaseCreate],
    And a successful call [to create a case] as in [FT_ConditionalPostState_Create_Case],
    And a successful call [to get an event token for the case just created] as in [S-122-GetToken_UpdateCase],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created],
    And the request [contains Update token created as in S-122-GetToken_UpdateCase],
    And it is submitted to call the [submit updateCase2 event with TextField and EmailField values] operation of [CCD data store],

    Then a positive response is received,
    And the response has all the details as expected

#-----------------------------------------------------------------------------------------------------------------------

#-----------------------------------------------------------------------------------------------------------------------
