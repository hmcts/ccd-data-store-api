#============================================
@F-051
Feature: F-051: Get default settings for user
#============================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-110
  Scenario: must return default user setting successfully for a user having a profile in CCD, endpoint: GET V1 /caseworkers/{uid}/profile

    Given a user with [a detailed profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-109
  Scenario: must return a list of jurisdictions for a valid user with no CCD user profile, endpoint: GET V1 /caseworkers/{uid}/profile

    Given a user with [no profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-108
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation, endpoint: GET V1 /caseworkers/{uid}/profile

    Given a user with [a detailed profile in CCD],

    When a request is prepared with appropriate values,
    And the request [uses an uid of a different user, with no profile access],
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [has the 403 return code],
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-107
  Scenario: must return 401 when request does not provide valid authentication credentials, endpoint: GET V1 /caseworkers/{uid}/profile

    Given a user with [a detailed profile in CCD],

    When a request is prepared with appropriate values,
    And the request [uses an invalid user token],
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [contains a 401 return code],
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-106
  Scenario: must return default user setting successfully for a user having a profile in CCD, endpoint: GET V2 /internal/profile

    Given a user with [a detailed profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-105
  Scenario: must return a list of jurisdictions for a valid user with no CCD user profile, endpoint: GET V2 /internal/profile

    Given a user with [no profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a positive response is received,
    And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-104
  Scenario: must return 401 when request does not provide valid authentication credentials, endpoint: GET V2 /internal/profile

    Given a user with [a detailed profile in CCD],

    When a request is prepared with appropriate values,
    And the request [uses an invalid user token],
    And it is submitted to call the [Get default settings for user] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [contains a 401 return code],
    And the response has all the details as expected.
