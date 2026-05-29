@F-943
Feature: F-943: Get closed cases

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a call [to delete existing date case closed records] will get the expected response as in [S-943.1_DeleteExistingDateCaseClosed],

  @S-943.1
  Scenario: Return 200 when get closed cases requested
    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
    And a successful call [to create a date case closed record] as in [S-943.1_CreateDateCaseClosed],
    And a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And the request [contains Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all other details as expected.

  @S-943.2
  Scenario: Return 400 if date validation fails
    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And the request [contains Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [code is HTTP-400 Bad Request],
    And the response has all the details as expected.

  @S-943.3
  Scenario: Return 401 if user is unauthorised
    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And the request [contains a dummy user id],
    And the request [contains Date field],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [code is HTTP-401],
    And the response has all the details as expected.

  @S-943.4
  Scenario: Return 404 if no closed cases are found
    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And the request [contains Date field for which there are no cases available],
    And it is submitted to call the [get closed cases] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [code is HTTP-404],
    And the response has all the details as expected.
