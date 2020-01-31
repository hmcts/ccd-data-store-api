@F-049
Feature: F-049: Start case creation as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-235
  Scenario: must start the case creation process successfully for correct inputs
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a positive response is received
    And     the response [code is HTTP-200]
    And     the response has all the details as expected

  @S-231 @Ignore
  Scenario: must return 401 when request provides invalid authentication credentials
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a dummy user id]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-401]
    And     the response has all the details as expected

  @S-231
  Scenario: must return 403 when the request contains a dummy user id
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a dummy user id]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-232
  Scenario: must return 403 when the request contains a jurisdiction id user is unauthorised to access
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a jurisdiction id user is unauthorised to access]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-233
  Scenario: must return 403 when request contains a malformed user ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a malformed user id]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-234 @Ignore
  Scenario: must return 400 when request contains a malformed Case Type ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a malformed Case Type ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-400]
    And     the response has all the details as expected

  @S-511 @Ignore
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a non-existing jurisdiction ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected

  @S-511
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a non-existing jurisdiction ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-512
  Scenario: must return 404 when request contains a non-existing Case type ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a non-existing Case type ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected

  @S-513
  Scenario: must return 404 when request contains a non-existing Event ID
    Given   a user with [an active profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [contains a non-existing Event ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected
