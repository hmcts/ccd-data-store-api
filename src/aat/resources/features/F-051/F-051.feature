@F-051
Feature: Get default settings for user

  @S-110
  Scenario: must return default user setting successfully for a user having a profile in CCD
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the get default settings for user operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected


  @S-109
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given an appropriate test context as detailed in the test data source
    And a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-107
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get default setting for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-108
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get default setting for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected
