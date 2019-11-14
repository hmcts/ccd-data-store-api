@F-048
Feature: Get the pagination metadata for a case data search for Case Worker

  @S-133
  Scenario: must return 400 if the sort direction in input parameters is not in ASC or DESC
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-134
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-135
  Scenario: must return negative response when request provides authentic credentials without authorized access to the operation
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
      And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-136
  Scenario: must return pagination metadata successfully for correct inputs
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected

  @S-515
  Scenario: must return 400 when casefields do not start with “case.”
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-516
  Scenario: must return 400 when security classification in input parameters is present and invalid
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get the pagination metadata for a case data search for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected
