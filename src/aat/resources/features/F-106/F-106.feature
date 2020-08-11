@F-106
Feature: F-106: Update Supplementary Data

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-605
  Scenario: Must return the updated supplementary data values from Data store
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F106_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Update Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.

  @S-606
  Scenario: Need to be able to increment an existing property
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F106_Case_Data_Create_C1],
    And a successful call [by Dil to update supplementary_data] as in [F-106_Update_Supplementary_Data_C1],
    When a request is prepared with appropriate values,
    And the request [contains increments of a specified value to an existing Supplementary Data property],
    And it is submitted to call the [Update Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.

  @S-607
  Scenario: Need to be able to replace an existing property
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F106_Case_Data_Create_C1],
    And a successful call [by Dil to update supplementary_data] as in [F-106_Update_Supplementary_Data_C1],
    When a request is prepared with appropriate values,
    And the request [replaces the value of an existing supplementary_data property with the provided value],
    And it is submitted to call the [Update Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.
