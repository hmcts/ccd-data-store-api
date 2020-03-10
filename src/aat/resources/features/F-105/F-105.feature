@F-105 @focus
Feature: F-105: Case Roles Access Management

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    # We should improve the framework and add a check that the CaseType CaseRoles2 is not in a json array instead of
    # checking that all other CaseTypes exist.
  @S-583
  Scenario: CREATOR role does not grant access to the CaseType when returning a list of jurisdictions for a valid user
    Given a user with [an active profile in CCD having create case access for a jurisdiction]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access parameter]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains HTTP 200 Ok status code]
    And the response [contains the list of jurisdictions a user has access to]
    And the response has all other details as expected

    # This is a good example for the operator: subset - not working atm
  @S-597
  Scenario: CREATOR role does not grant access to the CaseEvent when returning a list of jurisdictions for a valid user
    Given a user with [an active profile in CCD having create case access for a jurisdiction]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access parameter]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains HTTP 200 Ok status code]
    And the response [contains the list of jurisdictions a user has access to]
    And the response has all other details as expected
