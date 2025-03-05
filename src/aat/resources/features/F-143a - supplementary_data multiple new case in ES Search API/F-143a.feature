@F-143a @elasticsearch
Feature: F-143a Additional supplementary data property returned by ES Search APIs

  Background: Load test data for the scenario

    Given an appropriate test context as detailed in the test data source
    And a case that has just been created as in [F-143a_CreateCasePreRequisiteCaseworker_Multiple_Orgs]
    And a successful call [to add supplementary data for the case] as in [F-143a_Add_Supplementary_Data_Multiple_Orgs]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile]

  @S-143a.1
  Scenario: external search api returns multiple new case orgs supplementary data by default
    Given the request [is configured to search for the previously created case],
    And the request [does not explicitly request supplementary_data]
    And a request is prepared with appropriate values,
    When it is submitted to call the [External Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains supplementary data],
    Then the response [contains the specified sub section of new_case of supplementary data],
    And the response has all other details as expected.
