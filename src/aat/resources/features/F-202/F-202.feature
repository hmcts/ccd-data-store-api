@ash
Feature: Access Control Search Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-202.1
    Scenario: User cannot get a case that has higher SC than the users Role Assignemnt
      Given a user with [restricted access to create a case J1-CT2-01]
      And a user with [PUBLIC SC ORGANISATION role assignment access]
      And a case that has just been created as in [F202_Create_Case_CT1]
      And a successful call [to give user Solicitor1 a PUBLIC CASE role assignment to view the previously created case J1-CT2-01] as in [S-202.1_Grant_Role_Assignment]
      When a request is prepared with appropriate values
      And the request [attempts to search for case J1-CT2-01]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains no cases]
      And the response has all other details as expected

  @S-202.3
  Scenario: todo (3rd row on worked examples table)
    Given a user with [restricted access to create CT2 cases]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]

    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]

    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]

    @S-202.8
  Scenario: todo ( 6th and 8th row on worked examples table)
       Given a user with [restricted access to create CT6 cases]
       And a case that has just been created as in [J1-CT6-01]

      And a case that has just been created as in [J1-CT7-01]
      And a case that has just been created as in [J1-CT7-02]
      And a case that has just been created as in [J1-CT7-03]
      # When a request is prepared with appropriate values
      # And the request [attempts to search for case J1-CT6-01]
      # And it is submitted to call the [ES Search] operation of [CCD Data Store]
      # Then a positive response is received
      # Then the response [contains no cases]
      # And the response has all other details as expected



