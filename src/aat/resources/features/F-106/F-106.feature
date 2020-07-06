@F-106
Feature: F-106: Update Supplementary Data

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-605
  Scenario: when a specific User ID is supplied for a specific case ID, then the case roles relating only to the User ID for that specific Case ID must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F106_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & Dil's User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil, as per above],
    And the response has all other details as expected.

