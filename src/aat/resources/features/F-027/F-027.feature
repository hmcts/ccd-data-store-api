@F-027
Feature: Fetch a case for display for Case Worker

  @S-038
  Scenario: must return status 200 along with the CaseView object successfully
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected

  @S-035
  Scenario: must return appropriate negative response when case-reference does not exists
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-036
  Scenario: must return 4xx when request does not provide valid authentication credentials
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-037
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-034
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given an appropriate test context as detailed in the test data source
    And a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected
