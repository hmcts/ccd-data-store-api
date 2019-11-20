@F-057
Feature: Get the pagination metadata for a case data search for Citizen

  @S-137
  Scenario: must return 400 if the sort direction in input parameters is not in ASC or DESC
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-138
  Scenario: must return 4xx response when request does not provide valid authentication credentials
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-139
  Scenario: must return negative response when request provides authentic credentials without authorized access to the operation
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
      And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-140
  Scenario: must return pagination metadata successfully for correct inputs
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected

  @S-521
  Scenario: must return 400 when casefields do not start with “case.”
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-522
  Scenario: must return 400 when security classification in input parameters is present and invalid
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected
