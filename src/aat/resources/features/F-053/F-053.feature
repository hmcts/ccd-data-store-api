@F-053
Feature: F-053: Submit case creation as Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-269
  Scenario: must create case successfully and return positive response HTTP-201 for correct inputs
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [code is HTTP-201]
    And   the response has all other details as expected

  @S-271
    Scenario: must return negative response HTTP-403 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When  a request is prepared with appropriate values
    And   the request [does not provide valid authentication credentials]
    And   it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-272
  Scenario: must return negative response HTTP-403 when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When  a request is prepared with appropriate values
    And   the request [does not provide an authorised access to the operation]
    And   it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-273
  Scenario: must return negative response HTTP-422 when request contains an invalid jurisdiction ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains an invalid jurisdiction ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-422]
    And the response has all the details as expected

  @S-274
  Scenario: must return negative response HTTP-404 when request contains a non-existing case type ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a non-existing case type ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

  @S-275
  Scenario: must return negative response HTTP-403 when request contains a non-existing user ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a non-existing user ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all the details as expected

  @S-276 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed user ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a malformed user ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-400]
    And the response has all the details as expected

  @S-268 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed jurisdiction ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a malformed jurisdiction ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-400]
    And the response has all the details as expected

  @S-267 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed case type ID
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a malformed case type ID]
    And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-400]
    And the response has all the details as expected
