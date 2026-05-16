@F-1026
Feature: F-1026: Get jurisdictions available to the user

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1026.1
  Scenario: must return a list of jurisdictions for a valid user
    Given a user with [an active profile in CCD having create case access for a jurisdiction],
    When a request is prepared with appropriate values,
    And the request [has CREATE as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok status code],
    And the response [contains the list of jurisdictions a user has access to],
    And the response has all other details as expected.

  @S-1026.2
  Scenario: must return 400 if access type is not in create, read or update
    Given a user with [a detailed profile in CCD having create case access for a jurisdiction],
    When a request is prepared with appropriate values,
    And the request [has DELETE as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains HTTP 400 Bad Request],
    And the response [contains an error message : Access can only be 'create', 'read' or 'update'],
    And the response has all other details as expected.

  @S-1026.3
  Scenario: must return a list of jurisdictions for a valid user with no user profile
    Given a user with [appropriate idam roles but no CCD user profile],
    When a request is prepared with appropriate values,
    And the request [has CREATE as case access parameter],
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains the list of jurisdictions a user has access to],
    And the response has all other details as expected.
