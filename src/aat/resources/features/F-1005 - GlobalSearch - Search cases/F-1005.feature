@F-1005 @elasticsearch # Exclude all tests until issues with them are resolved
Feature: F-1005: Global Search - Search cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a successful call [to create the global search index] as in [F-1005_GlobalSearchIndexCreation]
    And a case that has just been created as in [F-1005_CreateCasePreRequisiteCaseworker]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]

  @S-1005.1 @AC1
  Scenario: Successfully search for case with one of the new global search parameters
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains at least one fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response has all other details as expected.

  @S-1005.2  @AC2
  Scenario: Successfully search for case with all of the new global search parameters
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains all of the fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response has all other details as expected.

  @S-1005.3  @AC3
  Scenario:  Unsuccessful search for case with none of the new global search parameters
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains none of the fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a negative response is received,
    And the response [has 400 return code],
    And the response [contains the error message 'Must have at least one search criteria'],
    And the response has all other details as expected.

  @S-1005.4  @AC4
  Scenario:  Unsuccessful search for case with invalid schema
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains at least one fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And the request [contains an invalid schema]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a negative response is received,
    And the response [has 400 return code],
    And the response [contains the error message 'Input not valid'],
    And the response has all other details as expected.

  @S-1005.5  @AC5
  Scenario:  Unsuccessful search for case with invalid global search parameters with an invalid user
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains at least one fields from new global search screen]
    And the request [contains all the mandatory parameters]
    And the request [contains a user that doesn't have access to the cases]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns 0 cases],
    And the response has all other details as expected.

  @S-1005.6
  Scenario: Successfully search for an updated case with the new values
    Given a user with [an active profile in CCD]
    And a successful call [to get a caseworker event token to update the case just created] as in [F-1005_GetCaseworkerUpdateToken]
    And a successful call [to update a case] as in [F-1005_UpdateCasePreRequisiteCaseworker]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [contains the field that has been updated]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns the updated case],
    And the response has all other details as expected.

  @S-1005.7
  Scenario: Successfully search for case using wildcards
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains one of the global search fields using a wildcard character]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [contains the case that has the value searched with the wildcards],
    And the response has all other details as expected.

  @S-1005.8
  Scenario: Successfully verifies the pagination
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1005_CreateSecondCasePreRequisiteCaseworker]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [contains the sortCriteria]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [contains the cases in correct order],
    And the response has all other details as expected.

  @S-1005.9
  Scenario: Successfully searches across different Case Types
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1005_CreateCaseMasterCaseType]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [contains two different Case Types]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [contains both cases],
    And the response has all other details as expected.

  @S-1005.10
  Scenario: Successfully searches for a case that was modified by a callback
    Given a user with [an active profile in CCD]
    And a successful call [to create a case] as in [F-1005_CreateCaseWithCallback]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [contains the fields modified by the callback]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [contains the case modified by the callback],
    And the response has all other details as expected.
