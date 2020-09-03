@F-105
Feature: F-105: Add Case-Assigned Users and Roles

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  # RDM-8606/8806 AC-1
  @S-105.1
  Scenario: Must successfully assign a user and case role for a specific case by a user calling through/from an authorised application
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-105.1_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-2
  @S-105.2
  Scenario: Must return an error response for a missing Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
    And the request [contains the Case ID of C1 in one entry but no case ID in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.2_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-3
  @S-105.3
  Scenario: Must return an error response for a malformed Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
    And the request [contains the Case ID of C1 in one entry and a malformed case ID in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.3_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-4
  @S-105.4
  Scenario: Must return an error response for a missing User ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
    And the request [contains the User ID of Olawale in one entry but no User ID in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.4_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-5
  @S-105.5
  Scenario: Must return an error response for a malformed User ID Provided
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing the Case ID of C1 and a proper Case Role CR-1],
    And the request [contains the User ID of Olawale in one entry and a malformed User ID in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.5_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-6
  @S-105.6
  Scenario: Must return an error response when the request is made from an un-authorised application
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    And a user [Dil - with an active profile],
    And a user [Olawale - who is not a privileged user and is calling from an un-authorised application],
    When a request is prepared with appropriate values,
    And the request [is made by Olawale with the Case ID of C1 & Dil's User ID and a proper Case Role CR-1],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Dil hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.6_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-7
  @S-105.7
  Scenario: Must return an error response for a malformed Case Role provided
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
    And the request [contains a proper Case Role CR-1 in one entry and an improper Case Role in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.7_Get_Case_Roles_for_Case_C1].

  # RDM-8606/8806 AC-8
  @S-105.8
  Scenario: Must return an error response for a missing Case Role
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
    And the request [contains a proper Case Role CR-1 in one entry and no Case Role in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.8_Get_Case_Roles_for_Case_C1].

  # RDM-8606 no list
  @S-105.9
  Scenario: Must return an error response for missing case_users list
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who is to add some case role assignment for a case],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application by Dil, with no case_users supplied],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected.

  # RDM-8606 empty list
  @S-105.10
  Scenario: Must return an error response for empty case_users list
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who is to add some case role assignment for a case],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application by Dil, with an empty list of case_users supplied],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected.

  # RDM-8606 case not found
  @S-105.11
  Scenario: Must return an error response when the case does not exist
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing Olawale's User ID and a proper Case Role CR-1],
    And the request [contains the Case ID of C1 in one entry and a well formed but non-existant case ID in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.11_Get_Case_Roles_for_Case_C1].


  # RDM-8606 duplicate
  @S-105.12
  Scenario: Must not create duplicate case-user-roles
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by Dil for 2 assignments each containing the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-105.12_Get_Case_Roles_for_Case_C1].

  # RDM-8606 multiple
  @S-105.13
  Scenario: Must successfully assign multiple user and case roles for a specific case by a user calling through/from an authorised application
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile and valid User ID],
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application by Dil, for 2 assignments each containing the Case ID of C1 and User ID of Olawale],
    And the request [contains a proper Case Role CR-1 in one entry and a proper Case Role CR-2 in the other],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's reception of the role CR-1 and CR-2 over the case C1] will get the expected response as in [S-105.13_Get_Case_Roles_for_Case_C1].

  # RDM-8842 AC-1
  @S-105.14
  Scenario: Must successfully increment Assigned User Count when assigning a user and case role for a specific case (by a user calling through/from an authorised application)
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Dil - who is to add some case role assignment for a case],
    And a user [Olawale - with an active solicitor profile],
    And a user [Hemanth - with an active solicitor profile],
    And a case [C1, which Richard has just] created as in [F-105_Case_Data_Create_C1],
    And a successful call [to check the number of users having access to C1 in its supplementary data] as in [F-105_Prerequisite_Counter_Check_Call],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and CR-2 and the Organisation ID of Olawale],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's reception of the role CR-1 and CR-2 over the case C1] will get the expected response as in [S-105.14_Verify_Case_Roles_for_Case_C1],
    And a call [to verify the count of users assigned to C1 has increased by 1] will get the expected response as in [S-105.14_Verify_Counter_1],
    And a call [to repeat the same request as above] will get the expected response as in [S-105.14_Repeated_Call_to_Add_Case_Assigned_Users_and_Roles],
    And a call [to verify the count of users assigned to C1 has NOT changed] will get the expected response as in [S-105.14_Verify_Counter_2],
    And a call [to repeat the same request as above this time with a different user, Hemanth] will get the expected response as in [S-105.14_Repeated_Call_to_Add_Case_Assigned_Users_and_Roles_Hemanth],
    And a call [to verify the count of users assigned to a case has increased by 1] will get the expected response as in [S-105.14_Verify_Counter_3].

   # RDM-8842 AC-2
   @S-105.15
   Scenario: Must not increment Assigned User Count when assigning a user and case role for a specific case if there was already a case user role assignment with the respective values in the request (by a user calling through/from an authorised application)
     Given an appropriate test context as detailed in the test data source,
     And a user [Richard - who can create a case],
     And a user [Dil - who is to add some case role assignment for a case],
     And a user [Olawale - with an active solicitor profile and valid User ID],
     And a case [C1, which Richard has just] created as in [F-105_Case_Data_Create_C1],
     And a successful call [to grant access for Olawale with a case role CR-1 over the case C1] as in [S-105.15_Grant_Access],
     And a successful call [to check the number of users having access to C1 in its supplementary data] as in [F-105_Prerequisite_Counter_Check_Call],
     When a request is prepared with appropriate values,
     And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-2 and the Organisation ID of Olawale],
     And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
     Then a positive response is received,
     And the response has all the details as expected,
     And a call [to verify Olawale's reception of the role CR-2 over the case C1] will get the expected response as in [S-105.15_Verify_Case_Roles_for_Case_C1],
     And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].

   # RDM-8842 AC-3
   @S-105.16
   Scenario: No organisation ID is provided by the user so Assigned User Count remains unchanged
     Given an appropriate test context as detailed in the test data source,
     And a user [Richard - who can create a case],
     And a user [Dil - who is to add some case role assignment for a case],
     And a user [Olawale - with an active solicitor profile and valid User ID],
     And a case [C1, which Richard has just] created as in [F-105_Case_Data_Create_C1],
     And a successful call [to check the number of users having access to C1 in its supplementary data] as in [F-105_Prerequisite_Counter_Check_Call],
     When a request is prepared with appropriate values,
     And the request [is made from an authorised application, by Dil, with the Case ID of C1, User ID of Olawale, proper Case Role CR-1 and no Organisation ID],
     And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
     Then a positive response is received,
     And the response has all the details as expected,
     And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-105.16_Verify_Case_Roles_for_Case_C1],
     And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].

   # RDM-8842 AC-4
   @S-105.17
   Scenario: Invalid Organisation ID provided
     Given an appropriate test context as detailed in the test data source,
     And a user [Richard - who can create a case],
     And a user [Dil - who is to add some case role assignment for a case],
     And a user [Olawale - with an active solicitor profile],
     And a user [Hemanth - with an active solicitor profile],
     And a case [C1, which Richard has just] created as in [F-105_Case_Data_Create_C1],
     And a successful call [to check the number of users having access to C1 in its supplementary data] as in [F-105_Prerequisite_Counter_Check_Call],
     When a request is prepared with appropriate values,
     And the request [is made from an authorised application, by Dil for 2 assignments each containing the Case ID of C1, User ID of Olawale and Hemanth and proper Case Role CR-1],
     And the request [contains a valid Organisation ID in one entry and an improper Organisation ID in the other],
     And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
     Then a negative response is received,
     And the response has all the details as expected,
     And a call [to verify that Olawale hasn't received the role CR-1 over the case C1] will get the expected response as in [S-105.17_Verify_Case_Roles_for_Case_C1],
     And a call [to verify the count of users assigned to a case has NOT changed] will get the expected response as in [F-105_Verify_Counter_Unchanged].
