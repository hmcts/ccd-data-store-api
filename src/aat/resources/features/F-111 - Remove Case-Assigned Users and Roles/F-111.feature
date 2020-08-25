@F-111
Feature: F-111: Remove Case-Assigned Users and Roles

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-111.1
  Scenario: must successfully remove a user and case role for a specific case by a user calling through/from an authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil, without an organisation context, to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's eventual loss of the role CR-1 over the case C1] will get the expected response as in [S-111.1_Get_Case_Roles_for_Case_C1_After_Remove].

  @S-111.2
  Scenario: must successfully remove multiple user and case roles for a specific case by a user calling through/from an authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Richard to create a case - C2] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment_C2],
      And a successful call [by Dil to add a Case Role - CR1 and CR2 on both C1 and C2 for Olawale] as in [S-111.2_Add_Case_Assigned_User_Roles_for_Case_C1_And_C2],
      And a successful call [to verify Olawale's reception of the roles CR1 and CR2 on both C1 and C2] as in [S-111.2_Get_Case_Roles_for_Case_C1_And_C2_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for four assignments that made as above],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's eventual loss of all the assignments made before] will get the expected response as in [S-111.2_Get_Case_Roles_for_Case_C1_And_C2_After_Remove].

  @S-111.13
  Scenario: must successfully decrease Assigned User Count when removing a user and case role for a specific case
    Given a user [Richard - who can create a case],
    And a user [Dil - who is to add and remove some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile],
    And a user [Hemanth - with an active solicitor profile],
    And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    And a successful call [by Dil, within the context of his organisation, to add a Case Role - CR1 on C1 for Olawale and Hemanth] as in [S-111.13_Add_Case_Assigned_User_Roles_for_Case_C1_With_Organisation],
    And a successful call [to verify Olawale's and Hemanth's reception of the role CR-1 over the case C1] as in [S-111.13_Get_Case_Roles_for_Case_C1_After_Add],
    And a successful call [to verify number of users in Dil's organisation accessing C1] as in [S-111.13_Verify_Counter_1],

    When a request is prepared with appropriate values,
    And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and the Organisation ID of Olawale],
    And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's loss of the role CR-1 over the case C1] will get the expected response as in [S-111.13_Get_Case_Roles_for_Case_C1_After_Remove],
    And a call [to verify the count of users assigned to C1 has decreased by 1] will get the expected response as in [S-111.13_Verify_Counter_2],
    And a call [to repeat the same request as above] will get the expected response as in [S-111.13_Repeat_Call_to_Remove_For_Ola],
    And a call [to verify the count of users unassigned to C1 has NOT changed] will get the expected response as in [S-111.13_Verify_Counter_3],
    And a call [to repeat the same request as above this time with a different user, Hemanth] will get the expected response as in [S-111.13_Repeat_Call_to_Remove_For_Hemanth],
    And a call [to verify the count of users assigned to a case has decreased by 1] will get the expected response as in [S-111.13_Verify_Counter_4].

  @S-111.14
  Scenario: must not decrease Assigned User Count when unassigning a user and case role for a specific case if there was already a different case user role assignment
    Given a user [Richard - who can create a case],
    And a user [Dil - who is to add and remove some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile],
    And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    And a successful call [by Dil, within the context of his organisation, to add 2 Case Roles CR1 and CR2 on C1 for Olawale] as in [S-111.14_Add_Case_Assigned_User_Roles_for_Case_C1_With_Organisation],
    And a successful call [to verify Olawale's reception of CR1 and CR2 over the case C1] as in [S-111.14_Get_Case_Roles_for_Case_C1_After_Add],
    And a successful call [to verify that 1 user has access to C1 in Dil's organisation] as in [S-111.14_Verify_User_Count_Assigned_To_Case_Equals_1],

    When a request is prepared with appropriate values,
    And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-2 and the Organisation ID of Olawale],
    And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's loss of the role CR-2 over the case C1] will get the expected response as in [S-111.14_Get_Case_Roles_for_Case_C1_After_Remove],
    And a call [to verify the count of users unassigned to C1 has NOT changed] will get the expected response as in [S-111.14_Verify_User_Count_Assigned_To_Case_Equals_1].

  @S-111.15
  Scenario: must not decrease Assigned User Count when when no organisation ID is provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil, within the context of his organisation, to add a Case Role - CR1 on C1 for Olawale] as in [S-111.15_Add_Case_Assigned_User_Roles_for_Case_C1_With_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [S-111.15_Get_Case_Roles_for_Case_C1_After_Add],
      And a successful call [to verify that 1 user has access to C1 in Dil's organisation] as in [S-111.15_Verify_User_Count_Assigned_To_Case_Equals_1],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and no Organisation ID],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's loss of the role CR-1 over the case C1] will get the expected response as in [S-111.15_Get_Case_Roles_for_Case_C1_After_Remove],
      And a call [to verify the count of users unassigned to a case has NOT changed] will get the expected response as in [S-111.15_Verify_User_Count_Assigned_To_Case_Equals_1].

  @S-111.3
  Scenario: must return an error response for a missing Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry but no case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.4
  Scenario: must return an error response for a empty Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry and a empty case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.5
  Scenario: must return an error response for a missing User ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
      And the request [contains the User ID of Olawale in one entry but no User ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.6
  Scenario: must return an error response for a malformed User ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
      And the request [contains the User ID of Olawale in one entry and a malformed User ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.7
  Scenario: must return an error response when the request is made from an un-authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Olawale with the Case ID of C1 & Dil's User ID and a proper Case Role CR-1],
      And the request [is made from an application authorised to call Data Store API, but not the Remove Case-Assigned Users and Roles operation],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.8
  Scenario: Must return an error response for a malformed Case Role
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
      And the request [contains a proper Case Role CR-1 in one entry and a malformed Case Role in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.9
  Scenario: must return an error response for a missing Case Role
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
      And the request [contains a proper Case Role CR-1 in one entry and no Case Role in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.10
  Scenario: must return an error response for missing case_users list
    Given a user [Dil - who is to add some case role assignment for a case],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, with no list of case assigned users and roles supplied],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected.

  @S-111.11
  Scenario: must return an error response for empty case_users list
    Given a user [Dil - who is to add some case role assignment for a case],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, with an empty list of case assigned users and roles supplied],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected.

  @S-111.12
  Scenario: must return an error response when the case does not exist
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1_Without_Organisation],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [F-111_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry and a well-formed but non-existent case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [F-111_Get_Case_Roles_for_Case_C1_After_Add].

  @S-111.16
  Scenario: must reject request when an empty Organisation ID is provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a user [Hemanth - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale and Hemanth] as in [S-111.16_Add_Case_Assigned_User_Roles_for_Case_C1],
      And a successful call [to verify Olawale's and Hemanth's reception of the role CR-1 over the case C1] as in [S-111.16_Get_Case_Roles_for_Case_C1_After_Add],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil for 2 assignments each containing the Case ID of C1, User ID of Olawale and Hemanth and proper Case Role CR-1],
      And the request [contains a valid Organisation ID in one entry and an empty Organisation ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale and Hemanth haven't lost the role CR-1 over the case C1] will get the expected response as in [S-111.16_Get_Case_Roles_for_Case_C1_After_Remove],
      And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [S-111.16_Verify_User_Count_Unchanged].
