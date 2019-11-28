@F-051
Feature: Get default settings for user

Background:
    Given an appropriate test context as detailed in the test data source

  @S-110
  Scenario: must return default user setting successfully for a user having a profile in CCD
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses the Case ID of the case just created]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all the details as expected


  @S-109
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses a Case ID that doesn’t exist in CCD]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-107
  Scenario: must return 4xx when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses a Case ID that doesn’t exist in CCD]
    And the request [uses the invalid authorization]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-108
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses the Case ID of the case just created]
    And the request [uses the invalid authorization]
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected
