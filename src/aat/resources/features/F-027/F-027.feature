@F-027
Feature: Fetch a case for display for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-038
  Scenario: must return status 200 along with the case-view object successfully
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [has a valid case-reference in input parameters]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all the details as expected

  @S-035
  Scenario: must return appropriate negative response when case-reference does not exists
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the valid case-reference in input parameters]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected

  @S-036
  Scenario: must return 4xx when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the valid authentication credentials]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-037
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the authorised access to the operation]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-034
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected
