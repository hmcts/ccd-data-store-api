@F-111 @Ignore
Feature: F-111: Remove Case-Assigned Users and Roles

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-111.1
  Scenario: must successfully remove a user and case role for a specific case by a user calling through/from an authorised application
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
      And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-111.1_Get_Case_Roles_for_Case_C1].

     When a request is prepared with appropriate values,
      And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify Olawale's eventual loss of the role CR-1 over the case C1] will get the expected response as in [S-111.1_Get_Case_Roles_for_Case_C1].

  @S-111.2
  Scenario: must return an error response for a missing Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
      And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-111.2_Get_Case_Roles_for_Case_C1].

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry but no case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.2_Get_Case_Roles_for_Case_C1].

  @S-111.3
  Scenario: must return an error response for a malformed Case ID
    Given a user [Richard - who can create a case],
      And a user [Dil - who is to add and remove some case role assignment for a case],
      And a user [Olawale - with an active solicitor profile],
      And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
      And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
      And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-111.3_Get_Case_Roles_for_Case_C1].

     When a request is prepared with appropriate values,
      And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
      And the request [contains the Case ID of C1 in one entry and a malformed case ID in the other],
      And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.3_Get_Case_Roles_for_Case_C1].
 
 @S-111.4
 Scenario: Must return an error response for a missing User ID
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.4_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
 And the request [contains the User ID of Olawale in one entry but no User ID in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.4_Get_Case_Roles_for_Case_C1].
 
 @S-111.5
 Scenario: Must return an error response for a malformed User ID Provided
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.5_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
 And the request [contains the User ID of Olawale in one entry and a malformed User ID in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.5_Get_Case_Roles_for_Case_C1].
 
 @S-111.6
 Scenario: Must return an error response when the request is made from an un-authorised application
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.6_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made by Olawale with the Case ID of C1 & Dil's User ID and a proper Case Role CR-1],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Dil hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.6_Get_Case_Roles_for_Case_C1].
 
 @S-111.7
 Scenario: Must return an error response for a malformed Case Role provided
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.7_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
 And the request [contains a proper Case Role CR-1 in one entry and an improper Case Role in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.7_Get_Case_Roles_for_Case_C1].
 
 @S-111.8
 Scenario: Must return an error response for a missing Case Role
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.8_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
 And the request [contains a proper Case Role CR-1 in one entry and no Case Role in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.8_Get_Case_Roles_for_Case_C1].
 
 @S-111.9
 Scenario: Must return an error response for missing case_users list
 Given a user [Dil - who is to add some case role assignment for a case],
 When a request is prepared with appropriate values,
 And the request [is made from an authorised application by Dil, with no case_users supplied],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected.
 
 @S-111.10
 Scenario: Must return an error response for empty case_users list
 Given a user [Dil - who is to add some case role assignment for a case],
 When a request is prepared with appropriate values,
 And the request [is made from an authorised application by Dil, with an empty list of case_users supplied],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected.
 
 @S-111.11
 Scenario: Must return an error response when the case does not exist
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.11_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
 And the request [contains the Case ID of C1 in one entry and a well formed but non-existant case ID in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a negative response is received,
 And the response has all the details as expected,
 And a call [to verify that Olawale hasn't lost the role CR-1 over the case C1] will get the expected response as in [S-111.11_Get_Case_Roles_for_Case_C1].
 
 @S-111.13
 Scenario: Must successfully remove multiple user and case roles for a specific case by a user calling through/from an authorised application
 Given a user [Richard - who can create a case],
 And a user [Dil - who is to add and remove some case role assignment for a case],
 And a user [Olawale - with an active solicitor profile],
 And a successful call [by Richard to create a case - C1] as in [F-111_Prerequisite_Case_Creation_Call_for_Case_Assignment],
 And a successful call [by Dil to add a Case Role - CR1 on C1 for Olawale]
 And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-
 111.13_Get_Case_Roles_for_Case_C1].
 When a request is prepared with appropriate values,
 And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
 And the request [contains a proper Case Role CR-1 in one entry and a proper Case Role CR-2 in the other],
 And it is submitted to call the [Remove Case-Assigned Users and Roles] operation of [CCD Data Store Api],
 Then a positive response is received,
 And the response has all the details as expected,
 And a call [to verify Olawale's eventual loss of the role CR-1 over the case C1] will get the expected response as in [S-
 111.1_Get_Case_Roles_for_Case_C1].