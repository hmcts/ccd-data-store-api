@F-1006 @elasticsearch
Feature: F-1006: Global Search - Access Control Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source,
    And a successful call [to create the global search index] as in [GlobalSearchIndexCreation],
    And a case that has just been created as in [F-1006_CreateCasePreRequisiteCaseworker],
    And a successful call [to create another four cases] as in [F-1006_CreateCases],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created].

  @S-1006.1 @AC1
  Scenario: Returning all results from Global Search matching the users jurisdictions/case types
    Given a user with [no field level RESTRICTED of security classification],
    When a request is prepared with appropriate values,
    And the request [contains relevant jurisdictions and case types],
    And it is submitted to call the [Global Search] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains only PUBLIC cases],
    And the response has all other details as expected.

  @S-1006.2  @AC2
  Scenario: Returning all results from Global Search matching the users jurisdictions/case types and excluding restricted cases
    Given a user with [no field level RESTRICTED of security classification],
    And a case that has just been created as in [F-1006_CreateRestrictedCase],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    When a request is prepared with appropriate values,
    And the request [contains relevant jurisdictions and case types],
    And it is submitted to call the [Global Search] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [does not contain any restricted classification results],
    And the response has all other details as expected.

  @S-1006.3  @AC3
  Scenario: Returning all results from Global Search matching the users jurisdictions/case types, excluding fields without Read permissions
    Given a user with [no READ access to a field],
    When a request is prepared with appropriate values,
    And the request [contains relevant jurisdictions and case types],
    And it is submitted to call the [Global Search] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [does not contain any fields from the case data for which the user doesn't have Read access],
    And the response has all other details as expected.

  @S-1006.4  @AC4
  Scenario:  Returning all results from Global Search matching the users jurisdictions/case types, excluding any case where the user doesn't have Read access for a search field that they supplied
    Given a user with [no READ access to a field],
    When a request is prepared with appropriate values,
    And the request [contains relevant jurisdictions and case types],
    And the request [contains the search field with no R access],
    And it is submitted to call the [Global Search] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [doesn't contain any cases],
    And the response has all other details as expected.

  @S-1006.5  @AC5
  Scenario: Returning all results from Global Search matching the users jurisdictions/case types and including restricted cases
    Given a user with [field level RESTRICTED of security classification],
    And a case that has just been created as in [F-1006_CreateRestrictedCase],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    When a request is prepared with appropriate values,
    And the request [contains relevant jurisdictions and case types],
    And it is submitted to call the [Global Search] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains the restricted case],
    And the response has all other details as expected.

