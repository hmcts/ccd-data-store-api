@F-053
Feature: F-053: Submit case creation as Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-269 # must create case successfully for correct inputs
  Scenario: must create case successfully and return positive response HTTP-201 for correct inputs
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When  a request is prepared with appropriate values
    And   the request [contains a token created as in F-053-Prerequisite]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [code is HTTP-201]
    And   the response has all other details as expected


  @S-270 @Ignore # wrong scenario in Excel
  Scenario: must return 201 if event creation is successful for a citizen

  @S-271 @Ignore # Response code mismatch, expected: 401, actual: 403
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [does not provide valid authentication credentials]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-401]
    And   the response has all other details as expected

  @S-272
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [provides authentic credentials without authorised access to the operation]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-273 @Ignore # Postponed.
  Scenario: must return 409 if case is altered outside of transaction


  @S-274 @Ignore # Postponed
  Scenario: must return 409 when case reference is not unique


  @S-275 @Ignore # Postponed
  Scenario: must return 422 if event trigger has failed


  @S-276 @Ignore # Postponed
  Scenario: must return 422 when process could not be started


  @S-267 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed case type ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed case type ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-268 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed jurisdiction ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed jurisdiction ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-550
  Scenario: must return negative response HTTP-422 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing jurisdiction ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-422]
    And   the response has all other details as expected

  @S-551
  Scenario: must return negative response HTTP-404 when request contains a non-existing case type ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing case type ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-404]
    And   the response has all other details as expected

  @S-552
  Scenario: must return negative response HTTP-403 when request contains a non-existing user ID
    Given a user with [an inactive profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing user ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-553 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed user ID
    Given a user with [an inactive profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed user ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected
