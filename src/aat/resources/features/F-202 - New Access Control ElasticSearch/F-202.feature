@ash
Feature: Access Control Search Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-202.1
    Scenario: User cannot search for a case that has a higher SC than the users Role Assignemnt
      Given a user [Solicitor1]
      Given a user with [superuser access to create PRIVATE case J1-CT1-01]
      And a case that has just been created as in [J1-CT1-01]
      And a case that has just been created as in [J1-CT1-02]
      And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
      And a successful call [to update the case] as in [J1-CT1-02_Issued]

      And a wait time of [10] seconds [to allow for Logstash to index the case just created],

      And a successful call [Solicitor1 a PUBLIC CASE role assignment to view the previously created case J1-CT2-01] as in [solicitor1_case_role_assignments_CT1]
      When a request is prepared with appropriate values
      And the request [attempts to search for case J1-CT1-01]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [does not contain J1-CT1-01]
      And the response has all other details as expected

#todo cant get this to work, can't return any cases by any user for CT-01
  @S-202.2
  Scenario: User searching for case by Field they don't have SC access to can access case without that field displayed
    Given a user [Solicitor3]
    And a case that has just been created as in [J1-CT1-01]
    And a successful call [to create a token for event creation] as in [J1-CT1-01_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-01_Issued]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    And a successful call [to give Solicitor3 a PRIVATE CASE role assignment to view the previously created case J1-CT1-01] as in [solicitor3_case_role_assignments]
    When a request is prepared with appropriate values
    And the request [attempts to search for case J1-CT1-01 by RESTRICTED Field F1]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    Then the response [contains the case J1-CT1-01]
    Then the response [does not contain the RESTRICTED field F1 for J1-CT1-01]
    And the response has all other details as expected


#todo radhika
    @S-202.8
  Scenario: todo ( 6th and 8th row on worked examples table)
      Given a user with [restricted access to create CT6 and CT7 cases]

      And a case that has just been created as in [J1-CT6-01]
      And a case that has just been created as in [J1-CT7-01]
      And a case that has just been created as in [J1-CT7-02]
      And a successful call [to create a token for event creation] as in [J1-CT7-02_Issue_Event_Token_Creation]
      And a successful call [to update the case] as in [J1-CT7-02_Issued]
      And a case that has just been created as in [J1-CT7-03]
      And a wait time of [10] seconds [to allow for Logstash to index the case just created],


      And a user [Solicitor2]
      And a successful call [to give user Solicitor2 their role case role assignments] as in [solicitor2_case_role_assignments]
      When a request is prepared with appropriate values
      And the request [attempts to search for cases of CT6 and CT7 case types]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains no cases]
      And the response has all other details as expected

#todo PASS
    @S-202.3
    Scenario: User Searching can only return cases that have access to the state for
      Given a user [Solicitor2]

      And a case that has just been created as in [J1-CT2-01]
      And a case that has just been created as in [J1-CT2-02]
      And a case that has just been created as in [J1-CT2-03]
      And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
      And a successful call [to update the case] as in [J1-CT2-03_Issued]
      And a case that has just been created as in [J1-CT2-04]
      And a case that has just been created as in [J1-CT2-05]
      And a case that has just been created as in [J1-CT2-06]
      And a wait time of [10] seconds [to allow for Logstash to index the case just created],


      And a successful call [to give Solicitor2 Respondent case role assignments to J1-CT2-01,J1-CT2-03] as in [solicitor2_case_role_assignments_CT2]
      When a request is prepared with appropriate values
      And the request [attempts to search for case type CT2 by field F2]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [does not contain case J1-CT2-01 as the user does not have access to the state]
      Then the response [contains case J1-CT2-03 which the user has state access to]
      Then the response [contains case field F3 only as the user only has SC access to this field]
      And the response has all other details as expected


#TODO FAIL - we get back all 6 cases - probably ct2 org access pulling them all back
#TODO is this sceanrio not like 202.2 where we get the case bacl for the other case without the field we dont have sc access to
#  @S-202.4
#  Scenario: USer Searching can only return cases that have SC access to searched field
#    Given a user [Solicitor1]
#
#    And a case that has just been created as in [J1-CT2-01]
#    And a case that has just been created as in [J1-CT2-02]
#    And a case that has just been created as in [J1-CT2-03]
#    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
#    And a successful call [to update the case] as in [J1-CT2-03_Issued]
#    And a case that has just been created as in [J1-CT2-04]
#    And a case that has just been created as in [J1-CT2-05]
#    And a case that has just been created as in [J1-CT2-06]
#    And a wait time of [10] seconds [to allow for Logstash to index the case just created],
#
#
#    And a successful call [to give Solicitor1 case role assignment to J1-CT2-02(PRIVATE) and J1-CT2-01(PUBLIC)] as in [solicitor1_case_role_assignments_CT2]
#    When a request is prepared with appropriate values
#    And it is submitted to call the [ES Search] operation of [CCD Data Store]
#    And the request [attempts to search for case type CT2 by PRIVATE field F2]
#    Then a positive response is received
#    And the response [contains J1-CT2-02 with private field F2]
#    And the response [does not contain J1-CT2-01 as the user does not have SC access to search on that field]
#
#    And the response has all other details as expected


    #todo PASS
  @S-202.5
  Scenario: User Searching can only return cases that have access to the state for
    Given a user [Solicitor1]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],


    And a successful call [to give Solicitor1 case role assignment to J1-CT2-02(PRIVATE) and J1-CT2-01(PUBLIC)] as in [solicitor1_case_role_assignments_CT2]
    When a request is prepared with appropriate values
    And the request [attempts to search for case type CT2 by PUBLIC field F3]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01 and J1-CT2-02]

    And the response has all other details as expected


  @S-202.6
  Scenario: User can search for case only configured with idam role
    Given a user [Solicitor1]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]

    And a case that has just been created as in [J1-CT7-01]
    And a case that has just been created as in [J1-CT7-02]
    And a successful call [to create a token for event creation] as in [J1-CT7-02_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT7-02_Issued]
    And a case that has just been created as in [J1-CT7-03]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    And a successful call [to give Solicitor1 case role assignment to J1-CT2-02(PRIVATE) and J1-CT2-01(PUBLIC)] as in [solicitor1_case_role_assignments_CT2]
    And a successful call [to give Solicitor1 case role assignment for J1-CT7-01] as in [solicitor1_case_role_assignments_CT7]
    When a request is prepared with appropriate values
    And the request [attempts to search for case type CT2,CT7 by PUBLIC field F3]

    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT7-01]

    And the response has all other details as expected

#todo PASS
    @S-202.7
    Scenario: User cant see cases in a state it does not have access to see
      Given a user [Solicitor2]

      Given a user with [restricted access to create CT2 cases]
      And a case that has just been created as in [J1-CT2-01]
      And a case that has just been created as in [J1-CT2-02]
      And a case that has just been created as in [J1-CT2-03]
      And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
      And a successful call [to update the case] as in [J1-CT2-03_Issued]
      And a case that has just been created as in [J1-CT2-04]
      And a case that has just been created as in [J1-CT2-05]
      And a case that has just been created as in [J1-CT2-06]
      And a wait time of [10] seconds [to allow for Logstash to index the case just created],


      And a successful call [to give Solicitor2 Respondent case role assignments to J1-CT2-01,J1-CT2-03] as in [solicitor2_case_role_assignments_CT2]
      When a request is prepared with appropriate values
      And the request [attempts to search for case type CT2 by field F3]
      And it is submitted to call the [ES Search] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains case J1-CT2-03]
      And the response has all other details as expected


      #todo why not J1-CT1-02 in the response? it's identical
      #todo no J1-CT1-01 - we should see this as we have PRIVATE org ra - NB CT1 Ccant seem to get working period
      #todo from worked example why not J1-CT1-03 J1-CT1-04
      #todo BROKEN we don't see any CT2 due to SC but as we have PRIVATE org ra but the cases are PUBLIC

  @S-202.9
  Scenario: User with ORG role assignment can search for cases they have access to
    Given a user [Staff1 with ORGANISATION role assignment to CT2(PUBLIC) and CT2(PRIVATE)]
    And a case that has just been created as in [J1-CT1-01]
    And a successful call [to create a token for event creation] as in [J1-CT1-01_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-01_Issued]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],


    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT1,CT2 by field F2]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04, J1-CT2-05, J1-CT2-06]
    And the response [contains J1-CT1-01]
    And the response has all other details as expected

    #todo FAIL - SHOWS CASES only the fields with sc access to are displayed
#  @S-202.10
#  Scenario: User with ORG role assignment gets no result when searching on field they don't have SC access to it
#    Given a user [Staff1 with ORGANISATION role assignment to CT2(PUBLIC) and CT2(PRIVATE)]
#    And a case that has just been created as in [J1-CT1-01]
#    And a successful call [to create a token for event creation] as in [J1-CT1-01_Update_Token_Creation]
#    And a successful call [to update the case] as in [J1-CT1-01_Issued]
#
#    And a case that has just been created as in [J1-CT1-02]
#    And a case that has just been created as in [J1-CT1-02]
#    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
#    And a successful call [to update the case] as in [J1-CT1-02_Issued]
#
#    And a case that has just been created as in [J1-CT2-01]
#    And a case that has just been created as in [J1-CT2-02]
#    And a case that has just been created as in [J1-CT2-03]
#    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
#    And a successful call [to update the case] as in [J1-CT2-03_Issued]
#    And a case that has just been created as in [J1-CT2-04]
#    And a case that has just been created as in [J1-CT2-05]
#    And a case that has just been created as in [J1-CT2-06]
#    And a wait time of [10] seconds [to allow for Logstash to index the case just created],
#
#    When a request is prepared with appropriate values
#    And the request [attempts to search for case types CT1,CT2 by field F4]
#    And it is submitted to call the [ES Search] operation of [CCD Data Store]
#    Then a positive response is received
#    And the response [contains no cases]
#    And the response has all other details as expected


    #todo BROKEN due to SC issue (case = public, RA = restricted)
  @S-202.11
  Scenario: User with ORG role assignment can search for cases by RESTRICTED field they have access to
    Given a user [Staff2 with ORGANISATION role assignment to CT2(RESTRICTED)]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2 by field F4]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04, J1-CT2-05, J1-CT2-06]
    And the response has all other details as expected


    #todo CT-05 has region attribute - should the staff 1 and 2 user not be able to see as they dont have the attribute in the scenarios above
  #todo BROKEN due to SC issue (case = public, RA = restricted)
  @S-202.12
  Scenario: User with can search for cases they have a matching Region attribute for
    Given a user [Staff5 with ORGANISATION role assignment to CT2(RESTRICTED) with region attribute]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2 by field F4]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04, J1-CT2-05, J1-CT2-06]
    And the response has all other details as expected

#todo BROKEN due to SC issue (case = public, RA = restricted)
  @S-202.13
  Scenario: User with can search for cases they have a matching Region and location attribute for
    Given a user [Staff6 with ORGANISATION role assignment to CT2(RESTRICTED) with region and location attribute]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],


    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2 by field F4]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04, J1-CT2-05, J1-CT2-06]
    And the response has all other details as expected

    #todo BROKEN due to SC issue (case = public, RA = restricted)
  @S-202.14
  Scenario: User cant search for case with a non matching Location attribute
    Given a user [Staff7 with ORGANISATION role assignment to CT2(RESTRICTED) with region and location attribute]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2 by field F4]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04, J1-CT2-05]
    And the response [does not contain J1-CT2-06 due to non matching Location attribute]
    And the response has all other details as expected

    #todo BROKEN due to SC issue (case = public, RA = restricted)
  @S-202.15
  Scenario: User cant search for case with a non matching Region attribute
    Given a user [Staff8 with ORGANISATION role assignment to CT2(RESTRICTED) with region and location attribute]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],


    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2 by field F4]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains J1-CT2-01, J1-CT2-02, J1-CT2-03, J1-CT2-04]
    And the response [does not contain J1-CT2-05, J1-CT2-06 due to non matching Region attribute]
    And the response has all other details as expected



    #todo how do we do this reduced SC stuff ?
#  @S-202.16
#  Scenario: User cant search for cases with a RA that's not configured for CaseType Access on the definition

  #todo pass
  @S-202.17
  Scenario: User cant search for case with a non matching Region attribute
    Given a user [Other1 with ORGANISATION role assignment to CT1,CT2]

    And a case that has just been created as in [J1-CT1-02]
    And a case that has just been created as in [J1-CT1-02]
    And a successful call [to create a token for event creation] as in [J1-CT1-02_Update_Token_Creation]
    And a successful call [to update the case] as in [J1-CT1-02_Issued]

    And a case that has just been created as in [J1-CT2-01]
    And a case that has just been created as in [J1-CT2-02]
    And a case that has just been created as in [J1-CT2-03]
    And a successful call [to create a token for event creation] as in [J1-CT2-03_Issue_Event_Token_Creation]
    And a successful call [to update the case] as in [J1-CT2-03_Issued]
    And a case that has just been created as in [J1-CT2-04]
    And a case that has just been created as in [J1-CT2-05]
    And a case that has just been created as in [J1-CT2-06]
    And a wait time of [10] seconds [to allow for Logstash to index the case just created],

    When a request is prepared with appropriate values
    And the request [attempts to search for case types CT2,CT1 by field F3]
    And it is submitted to call the [ES Search] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected




