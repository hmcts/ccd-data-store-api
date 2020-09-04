#==============================================================
@F-052
Feature: F-052: Get case data for a given case type for Citizen
#==============================================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-070  # Refactor this add actual Case data check, maybe best to create a case fist before checking for case data
Scenario: must return 200 and a list of case data for the given search criteria

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains correct input path variables],
      And it is submitted to call the [get case data for a given case type for citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains the case details, along with an HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-071 @Ignore # Response code mismatch, expected: 401, actual: 403 RDM-6628
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [get case data for a given case type for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP 401 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-072 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide authorised access to the operation],
      And it is submitted to call the [get case data for a given case type for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-069 @Ignore # This scenario is returning 400 instead of expected 404, linked to defect JIRA-6918
Scenario: must return 404 when request contains a non-existing case type

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case type],
      And it is submitted to call the [get case data for a given case type for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP 404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
