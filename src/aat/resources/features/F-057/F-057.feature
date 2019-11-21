@F-057
Feature: Get the pagination metadata for a case data search for Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-140
  Scenario: must return pagination metadata successfully for correct inputs
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all the details as expected

  @S-137
  Scenario: must return 400 if the sort direction in input parameters is not in ASC or DESC
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the valid sort direction in input parameters]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected

  @S-138
  Scenario: must return 4xx response when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-139
  Scenario: must return negative response when request provides authentic credentials without authorized access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authorized access to the operation]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-521
  Scenario: must return 400 when casefields do not start with “case.”
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid casefields which starts with “case.”]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected

  @S-522
  Scenario: must return 400 when security classification in input parameters is present and invalid
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid security classification in input parameters]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected
