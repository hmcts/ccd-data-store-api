@F-134 @crud
Feature: F-134: Get CaseType with access Internal API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-134.1 @Ignore
  Scenario: User getting Profile with no CaseEvent R access has that event filtered out of the response
    Given a user with [an active profile in CCD having read case access for a jurisdiction]
    When a request is prepared with appropriate values,
    And the request [has READ as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response [contains the list of jurisdictions a user has access to],
    And the response has all other details as expected,
    And the response [does not contain the event5 of FT_CRUD case type with no R CRUD access].

  @S-134.2 @Ignore
  Scenario: User getting Profile with no CaseState R access has that state filtered out of the response
    Given a user with [an active profile in CCD having read case access for a jurisdiction]
    When a request is prepared with appropriate values,
    And the request [has READ as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response [contains the list of jurisdictions a user has access to],
    And the response has all other details as expected,
    And the response [does not contain the state3 of FT_CRUD case type with no R CRUD access].

  @S-134.3 @Ignore
  Scenario: User getting Profile with no CaseType R access has that caseType filtered out of the response
    Given a user with [an active profile in CCD having read case access for a jurisdiction]
    When a request is prepared with appropriate values,
    And the request [has READ as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response [contains the list of jurisdictions a user has access to],
    And the response has all other details as expected,
    And the response [does not contain the case type FT_CRUD_2 with no R CRUD access].


