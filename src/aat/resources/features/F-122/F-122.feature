@F-122 @elasticsearch # Fix for LAST_STATE_MODIFIED_DATE coming in 19.1
Feature: F-122: External Search API Test for search by reference

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    #possitive request scenario of each type
  @S-122.1
  Scenario: Usecase request using SearchResultsFields useCase returns correct fields
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid user profile],
    When the request [is configured to search for the previously created case via exact match],
    And the request [is using the query parameter use_case=search],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains the field headers as specified in the SearchResultsFields only],
    And the response [contains the field data as specified in the SearchResultsFields],
    And the response [contains the field data of all meta data fields],
    And the response has all other details as expected.
