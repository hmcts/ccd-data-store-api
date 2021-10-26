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



    @S-202.8
  Scenario: todo ( 6th and 8th row on worked examples table)
      Given a user with [restricted access to create CT6 and CT7 cases]

      And a case that has just been created as in [J1-CT6-01]
      And a case that has just been created as in [J1-CT7-01]
      And a case that has just been created as in [J1-CT7-02]
      And a successful call [to create a token for event creation] as in [J1-CT7-02_Update_Token_Creation]
      And a successful call [to update the case] as in [J1-CT7-02_Issued]
      And a case that has just been created as in [J1-CT7-03]

      And a user [Solicitor2]
      And a successful call [to give user Solicitor2 their role case role assignments] as in [solicitor2_case_role_assignments]
      When a request is prepared with appropriate values
      And the request [attempts to search for cases of CT6 and CT7 case types]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains no cases]
      And the response has all other details as expected


    @S-202.3
    Scenario: USer Searching can only return cases that have access to the state for
      Given a user with [restricted access to create CT2 cases]

      And a case that has just been created as in [J1-CT2-01]
      And a case that has just been created as in [J1-CT2-02]
      And a case that has just been created as in [J1-CT2-03]
      And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
      And a successful call [to update the case] as in [J1-CT2-03_Issued]
      And a case that has just been created as in [J1-CT2-04]
      And a case that has just been created as in [J1-CT2-05]
      And a case that has just been created as in [J1-CT2-06]

      And a successful call [to give user Solicitor2 their role case role assignments] as in [solicitor2_case_role_assignments_tmp]
      When a request is prepared with appropriate values
      And the request [attempts to search for case type CT2 by field F2]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [does not contain case J1-CT2-01 as the user does not have access to the state]
      Then the response [contains case J1-CT2-03 which the user has state access to]
      Then the response [contains case field F3 only as the user only has SC access to this field]
      And the response has all other details as expected



  @S-202.4
  Scenario: USer Searching can only return cases that have access to the state for
    Given a user with [restricted access to create CT2 cases]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]

    And a successful call [to give user Solicitor1 their role case role assignments] as in [solicitor1_case_role_assignments_tmp]
    When a request is prepared with appropriate values
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected


    #todo needs closer look at why the expected behaviour is what it is before finishing this test
  @S-202.5
  Scenario: USer Searching can only return cases that have access to the state for
    Given a user with [restricted access to create CT2 cases]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]

    And a successful call [to give user Solicitor1 their role case role assignments] as in [solicitor1_case_role_assignments_tmp]
    When a request is prepared with appropriate values
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-202.6
  Scenario: S-202.6
    Given a user with [restricted access to create CT2 cases]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]

    And a case that has just been created as in [J1-CT7-01]
    And a case that has just been created as in [J1-CT7-02]
    And a successful call [to create a token for event creation] as in [J1-CT7-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT7-02_Issued]
    And a case that has just been created as in [J1-CT7-03]

    And a successful call [to give user Solicitor1 their role case role assignments] as in [solicitor1_case_role_assignments_tmp]
    And a successful call [to give user Solicitor1 their role case role assignments] as in [solicitor1_case_role_assignments_CT7]
    When a request is prepared with appropriate values
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected


    @S-202.7
    Scenario: User cant see cases in a state it does not have access to see
      Given a user with [restricted access to create CT2 cases]
      And a case that has just been created as in [J1-CT2-01]
      And a case that has just been created as in [J1-CT2-02]
      And a case that has just been created as in [J1-CT2-03]
      And a successful call [to create a token for event creation] as in [J1-CT2-03_Update_Token_Creation]
      And a successful call [to update the case] as in [J1-CT2-03_Issued]
      And a case that has just been created as in [J1-CT2-04]
      And a case that has just been created as in [J1-CT2-05]
      And a case that has just been created as in [J1-CT2-06]

      And a successful call [to give user Solicitor2 their role case role assignments] as in [solicitor2_case_role_assignments_tmp]
      When a request is prepared with appropriate values
      And the request [attempts to search for case type CT2 by field F3]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains case J1-CT2-03]
      And the response has all other details as expected








