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
      And a successful call [by Dil, without an organisation context, to add a Case Role - CR1 on C1 for Olawale] as in [F-111_Add_Case_Assigned_User_Roles_for_Case_C1],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [S-111.1_Get_Case_Roles_for_Case_C1],
      # TODO : check with Mutlu if any re-wording is required as original case doesn't have organisation context / supplementary data
#      And a successful call [to verify number of users in Dil's organisation accessing C1 is zero] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's eventual loss of the role CR-1 over the case C1] will get the expected response as in [S-111.1_Get_Case_Roles_for_Case_C1_After_Remove].
#      And a call [to verify number of users in Dil's organisation accessing C1 is zero] will get the expected response as in [the respective test data file],

  @S-111.2
  @Ignore
  Scenario: must successfully remove multiple user and case roles for a specific case by a user calling through/from an authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Richard to create a case - C2] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 and C2 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],
      And a successful call [to verify number of users in Dil's organisation accessing C1 is zero] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and C2, and User ID of Olawale],
      And the request [contains a proper Case Role CR-1 in one entry and a proper Case Role CR-2 in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's eventual loss of the role CR-1 over the case C1] will get the expected response as in [S-111.1_Get_Case_Roles_for_Case_C1].
      And a call [to verify number of users in Dil's organisation accessing C1 is zero] will get the expected response as in [the respective test data file],
 
  @S-111.13
  @Ignore
  Scenario: must successfully decrease Assigned User Count when removing a user and case role for a specific case
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a user [Hemanth - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil, within the context of his organisation, to add a Case Role - CR1 on C1 for Olawale and Hemanth] as in [the respective test data file],
      And a successful call [to verify Olawale's and Hemanth's reception of the role CR-1 over the case C1] as in [the respective test data file],
      And a successful call [to verify number of users in Dil's organisation accssing C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and CR-2 and the Organisation ID of Olawale],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's loss of the role CR-1 and CR-2 over the case C1] will get the expected response as in [S-111.14_Verify_Case_Roles_for_Case_C1],
      And a call [to verify the count of users assigned to C1 has decreased by 1] will get the expected response as in [S-111.14_Verify_Counter_1],
      And a call [to repeat the same request as above] will get the expected response as in [S-111.14_Repeated_Call_to_Add_Case_Assigned_Users_and_Roles],
      And a call [to verify the count of users unassigned to C1 has NOT changed] will get the expected response as in [S-111.14_Verify_Counter_2],
      And a call [to repeat the same request as above this time with a different user, Hemanth] will get the expected response as in [S-111.14_Repeated_Call_to_Add_Case_Assigned_Users_and_Roles_Hemanth],
      And a call [to verify the count of users assigned to a case has decreased by 1] will get the expected response as in [S-111.14_Verify_Counter_3].
 
  @S-111.14
  @Ignore
  Scenario: must not decrease Assigned User Count when unassigning a user and case role for a specific case if there was already a different case user role assignment
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil, within the context of his organisation, to add 2 Case Roles CR1 and CR2 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of CR1 and CR2 over the case C1] as in [the respective test data file],
      And a successful call [to verify that 1 user has access to C1 in Dil's organisation ] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-2 and the Organisation ID of Olawale],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's loss of the role CR-2 over the case C1] will get the expected response as in [S-105.15_Verify_Case_Roles_for_Case_C1],
      And a call [to verify the count of users unassigned to C1 has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].

  @S-111.15
  @Ignore
  Scenario: must not decrease Assigned User Count when when no organisation ID is provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil, within the context of his organisation, to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],
      And a successful call [to verify that 1 user has access to C1 in Dil's organisation ] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and no Organisation ID],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's loss of the role CR-1 over the case C1] will get the expected response as in [S-105.16_Verify_Case_Roles_for_Case_C1],
      And a call [to verify the count of users unassigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].

  @S-111.17
  @Ignore
  Scenario: must not decrease the Assigned User Count to the case when removing only some but not all of the roles for a user
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 anc CR2 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 and CR2 over the case C1] as in [the respective test data file],
      And a successful call [to verify that 1 user has access to C1 in Dil's organisation ] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale and proper Case Role CR-2],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].

  @S-111.3
  @Ignore
  Scenario: must return an error response for a missing Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [the respective test data file],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry but no case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [the respective test data file].

  @S-111.4
  @Ignore
  Scenario: must return an error response for a malformed Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [the respective test data file],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry and a malformed case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [the respective test data file].
 
  @S-111.5
  @Ignore
  Scenario: must return an error response for a missing User ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [the respective test data file],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
      And the request [contains the User ID of Olawale in one entry but no User ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [the respective test data file].
 
  @S-111.6
  @Ignore
  Scenario: must return an error response for a malformed User ID Provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
      And the request [contains the User ID of Olawale in one entry and a malformed User ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 
     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.5_Get_Case_Roles_for_Case_C1].

  @S-111.7
  @Ignore
  Scenario: must return an error response when the request is made from an un-authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Olawale with the Case ID of C1 & Dil's User ID and a proper Case Role CR-1],
      And the request [is made from an application authorised to call Data Store API, but not the Remove Case-Assigned Users and Roles operation],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.6_Get_Case_Roles_for_Case_C1].
 
  @S-111.8
  @Ignore
  Scenario: Must return an error response for a malformed Case Role provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
      And the request [contains a proper Case Role CR-1 in one entry and a malformed Case Role in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.7_Get_Case_Roles_for_Case_C1].
 
  @S-111.9
  @Ignore
  Scenario: must return an error response for a missing Case Role
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
      And the request [contains a proper Case Role CR-1 in one entry and no Case Role in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.8_Get_Case_Roles_for_Case_C1].
 
  @S-111.10
  @Ignore
  Scenario: must return an error response for missing case_users list
    Given a user [Dil - who is to add some case role assignment for a case],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, with no list of case assigned users and roles supplied],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected.
 
  @S-111.11
  @Ignore
  Scenario: must return an error response for empty case_users list
    Given a user [Dil - who is to add some case role assignment for a case],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application by Dil, with an empty list of case assigned users and roles supplied],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected.
 
  @S-111.12
  @Ignore
  Scenario: must return an error response when the case does not exist
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale] as in [the respective test data file],
      And a successful call [to verify Olawale's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry and a well-formed but non-existant case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.11_Get_Case_Roles_for_Case_C1].

  @S-111.16
  @Ignore
  Scenario: must reject request when an invalid Organisation ID is provided
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a user [Hemanth - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale and Hemanth] as in [the respective test data file],
      And a successful call [to verify Olawale's and Hemanth's reception of the role CR-1 over the case C1] as in [the respective test data file],

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil for 2 assignments each containing the Case ID of C1, User ID of Olawale and Hemanth and proper Case Role CR-1],
      And the request [contains a valid Organisation ID in one entry and an improper Organisation ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-105.17_Verify_Case_Roles_for_Case_C1],
      And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].
