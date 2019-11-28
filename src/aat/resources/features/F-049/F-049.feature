@F-049
Feature: Start case creation as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-235
  Scenario: must start case creation process successfully for correct inputs
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide valid details]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a positive response is received
    And     the response [code is HTTP-200]
    And     the response has all the details as expected

  @S-231 @Ignore
  Scenario: must return 401 when request provide invalid authentication credentials
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid authentication credentials]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-401]
    And     the response has all the details as expected

  @S-231
  Scenario: must return 403 when request provide invalid authentication credentials
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid authentication credentials]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-232
  Scenario: must return 403 when request provide unauthorized access
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide unauthorized access]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-233 @Ignore
  Scenario: must return 404 when request provide invalid Idam user ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Idam user ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-233
  Scenario: must return 403 when request provide invalid Idam user ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Idam user ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-234 @Ignore
  Scenario: must return 400 when request provide special character for Case type ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide Special character for Case type ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-400]
    And     the response has all the details as expected

  @S-511 @Ignore
  Scenario: must return 404 when request provide invalid Jurisdiction ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Jurisdiction ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected

  @S-511
  Scenario: must return 403 when request provide invalid Jurisdiction ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Jurisdiction ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-403]
    And     the response has all the details as expected

  @S-512
  Scenario: must return 404 when request provide invalid Case type ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Case type ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected

  @S-513
  Scenario: must return 404 when provide invalid Event ID in CCD
    Given   a user with [a detailed profile in CCD]
    When    a request is prepared with appropriate values
    And     the request [provide invalid Event ID]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response [code is HTTP-404]
    And     the response has all the details as expected
