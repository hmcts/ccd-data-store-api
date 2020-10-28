@F-103
Feature: F-103: Get Case-Assigned Users and Roles

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-597
  Scenario: when a specific User ID is supplied for a specific case ID, then the case roles relating only to the User ID for that specific Case ID must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil, as per above],
    And the response has all other details as expected.

  @S-598
  Scenario: when the invoking user is not a privileged user but the request includes his/her own User ID, then the invoker's case roles for the case should be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Dil with the Case ID of C1 & Dil's own User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil, as per above],
    And the response has all other details as expected.

  @S-599
  Scenario: when no User ID is supplied for a specific case ID, then the case roles relating to all  users with access to that case must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Steve - with an active profile],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    And a successful call [by Jamal to assign Steve a few case roles to access C1] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & no User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil & Steve, as per above],
    And the response has all other details as expected.

  @S-600
  Scenario: when no User ID is supplied for a list of Case IDs, then the case roles relating to all users with access to all listed cases must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a case [C2, which has just been] created as in [F103_Case_Data_Create_C2],
    And a case [C3, which has just been] created as in [F103_Case_Data_Create_C3],
    And a user [Dil - with an active profile],
    And a user [Steve - with an active profile],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    And a successful call [by Jamal to assign Dil a few case roles to access C2] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C2],
    And a successful call [by Jamal to assign Dil a few case roles to access C3] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C3],
    And a successful call [by Jamal to assign Steve a few case roles to access C1] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C1],
    And a successful call [by Jamal to assign Steve a few case roles to access C2] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C2],
    And a successful call [by Jamal to assign Steve a few case roles to access C3] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C3],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with Case IDs of C1, C2 & C3 & no User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil & Steve for C1, C2 & C3, as per above],
    And the response has all other details as expected.

  @S-601
  Scenario: must return an error response for a missing Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - with a valid User ID],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with no Case ID & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.


  @S-602
  Scenario: must return an error response for a malformed Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - with a valid User ID],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with a malformed Case ID & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.

  @S-603
  Scenario: must return an error response for a malformed User ID List (e.g. user1,user2,,user4)
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Jamal - who is a privileged user with permissions to access the case assignments of other users],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & a malformed User ID list],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.

  @S-604
  Scenario: must return an error response when the invoker does not have the required IDAM role(s) to query the role assignments for users listed in the query
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Steve - who is not a privileged user and does not have permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Steve with the Case ID of C1 & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.

