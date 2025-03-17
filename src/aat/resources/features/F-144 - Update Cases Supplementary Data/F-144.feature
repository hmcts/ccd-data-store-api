@F-144
Feature: F-144: Update Cases Supplementary Data

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-144.1
  Scenario: Must return the updated supplementary data values from Data store
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F-144_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Update Cases Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.

  @S-144.2
  Scenario: Need to be able to update an existing property for multiple cases
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F-144_Case_Data_Create_C1],
    And a case [C2, which has just been] created as in [F-144_Case_Data_Create_C2],
    When a request is prepared with appropriate values,
    And the request [contains updates of a specified value to an existing Supplementary Data property],
    And it is submitted to call the [Update Cases Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.

  @S-144.3
  Scenario: Need to be able to replace an existing property
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F-144_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And the request [replaces the value of an existing supplementary_data property with the provided value],
    And it is submitted to call the [Update Cases Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.

