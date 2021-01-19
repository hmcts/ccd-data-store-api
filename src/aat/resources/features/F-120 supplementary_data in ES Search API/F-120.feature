@F-120 @elasticsearch
Feature: F-120 Additional supplementary data property returned by ES Search APIs

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a successful call [to add supplementary data for the case] as in [F-120_Add_Supplementary_Data]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile]

  @S-120.1 @Smoke
  Scenario: external search api returns supplementary data by default
    Given the request [is configured to search for the previously created case],
    And the request [does not explicitly request supplementary_data]
    And a request is prepared with appropriate values,
    When it is submitted to call the [External Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains supplementary data],
    And the response has all other details as expected.

  @S-120.2 @Ignore
  Scenario: standard internal search api returns supplementary data by default
    Given the request [is configured to search for the previously created case],
    And the request [does not explicitly request supplementary_data]
    And a request is prepared with appropriate values,
    When it is submitted to call the [Internal Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains supplementary data],
    And the response has all other details as expected.

  @S-120.3
  Scenario: internal search api usecase request does not return supplementary data by default
    Given the request [is configured to search for the previously created case],
    And the request [does not explicitly request supplementary_data]
    And the request [is using the query parameter use_case=orgcases],
    And a request is prepared with appropriate values,
    When it is submitted to call the [Internal Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains supplementary data],
    And the response has all other details as expected.

  @S-120.4
  Scenario: internal search api usecase request does return supplementary data when requested in the request
    Given the request [is configured to search for the previously created case],
    And the request [is configured to request supplementary_data]
    And the request [is using the query parameter use_case=orgcases],
    And a request is prepared with appropriate values,
    When it is submitted to call the [Internal Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains supplementary data],
    And the response has all other details as expected.

  @S-120.5
  Scenario: can request sub selection of supplementary data
    Given a user with [a valid profile]
    When the request [is configured to search for the previously created case],
    And the request [requests a subsection of the supplementary data]
    And a request is prepared with appropriate values,
    And it is submitted to call the [External Elastic Search Endpoint] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    Then the response [contains the specified sub section of supplementary data],
    And the response has all other details as expected.

