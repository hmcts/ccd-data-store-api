@F-026
Feature: F-026: Get a case data with UI layout

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-078
  Scenario: must return the list of cases and status code 200 for correct inputs
    Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [Get a case data with UI layout] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the details of the case just created, along with an HTTP-200 OK]
    And the response has all other details as expected

  @S-077
  Scenario: must return an empty SearchResultView envelope and status code 200 if case type is not found
    Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And the request [uses a case-type which is not in CCD]
    And it is submitted to call the [Get a case data with UI layout] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains an empty SearchResultView, along with an HTTP-200 OK]
    And the response has all other details as expected

  @S-074
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Get a case data with UI layout] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has an HTTP-403 return code]
    And the response has all other details as expected

  @S-075
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Get a case data with UI layout] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has an HTTP-403 return code]
    And the response has all other details as expected
