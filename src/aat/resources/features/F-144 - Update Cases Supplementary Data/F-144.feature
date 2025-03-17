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

  @S-144.4 @elasticsearch @Ignore
  Scenario: Must return the updated supplementary data values from Data store and search through elastic search
    Given a case [C1, which has just been] created as in [F-144_Case_Data_Create_C1],
    And a successful call [by Dil to update supplementary_data] as in [F-144_Set_Supplementary_Data_C1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for the previously created case by the updated supplementary data value],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-144.5 @elasticsearch @Ignore
  Scenario: Need to be able to decrement an existing property and search through elastic search
    Given a case [C1, which has just been] created as in [F-144_Case_Data_Create_C1],
    And a successful call [by Dil to update supplementary_data] as in [F-144_Set_Supplementary_Data_C1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for the previously created case by the updated supplementary data value],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.
