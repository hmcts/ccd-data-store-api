#================================================================
@F-063
Feature: F-063: Retrieve Search Input Details for Dynamic Display
#================================================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-215
Scenario: should retrieve search inputs for dynamic display

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a valid case type],
      And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains the correct search inputs for the given case type, along with an HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-213
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid user authentication token],
      And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 401 Unauthorised],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-214 # scenario changed from expecting a 403 Forbidden to expecting a 404 Not Found
Scenario: must return 404 when request provides authentic credentials without authorised access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an authentication token for a user that does not have read access to the case type],
      And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-561
Scenario: must return 404 when case type does not exist

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a case type that does not exist],
      And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-595  # test to check search input that includes LAST_STATE_MODIFIED_DATE
Scenario: should retrieve search inputs for dynamic display that includes LAST_STATE_MODIFIED_DATE

    Given a user with [an active profile in CCD]

     When a request is prepared with appropriate values
      And the request [contains a valid case type]
      And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]

     Then a positive response is received
      And the response [contains the correct search inputs for the given case type, along with an HTTP 200 OK]
      And the response [body has LAST_STATE_MODIFIED_DATE as one of the items]
      And the response has all other details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-219
 Scenario: Public_User should not retrieve search inputs for Private_CaseType's dynamic display

  Given a user with [an active Public profile in CCD],

  When a request is prepared with appropriate values,
  And the request [contains a valid Private case type],
  And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

  Then a positive response is received,
  And the response [does not contain the search inputs for the given Private case type, along with an HTTP 200 OK],
  And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 @S-220
 Scenario: Private_User should retrieve search inputs for Private_CaseType's dynamic display

  Given a user with [an active Private profile in CCD],

  When a request is prepared with appropriate values,
  And the request [contains a valid Private case type],
  And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store],

  Then a positive response is received,
  And the response [contains the correct search inputs for the given Private case type, along with an HTTP 200 OK],
  And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
