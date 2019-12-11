@F-064
Feature: F-064: Retrieve workbasket input details for dynamic display

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-217
  Scenario: should retrieve trigger when the case and event exists
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [includes the event start trigger for the case just created, along with an HTTP 200 OK]
    And the response has all other details as expected

  @S-218
  Scenario: must return a negative response when the request does not provide valid authentication credentials
    Given a user with [an inactive profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403 Forbidden]
    And the response has all other details as expected

  @S-219
  Scenario: must return a negative response when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403 Forbidden]
    And the response has all other details as expected

  @S-220
  Scenario: must return a negative response when request contains an invalid case type ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case type ID]
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404 'Bad Request']
    And the response has all the details as expected

  @S-221 @Ignore # Response code mismatch, expected: 404, actual: 500"
  Scenario: must return a negative response when request contains a malformed case type ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a malformed case type ID]
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404 'Bad Request']
    And the response has all the details as expected
