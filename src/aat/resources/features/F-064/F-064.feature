#=====================================================================
@F-064
Feature: F-064: Retrieve Work Basket Input Details for Dynamic Display
#=====================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-558
Scenario: must retrieve workbasket input details for dynamic display successfully

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-217 @Ignore # Response code mismatch, expected: 401, actual: 403
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-401 Unauthorised],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-218 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [provides authentic credentials without authorised access to the operation],
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-219 @Ignore # this scenario will later be refactored from previous implementation.
Scenario: should retrieve search inputs

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-220 @Ignore # this scenario will later be refactored from previous implementation.
Scenario: should retrieve workbasket inputs

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-556
Scenario: must return a negative response when request contains a non-existing case type id

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case type id],
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404 'Bad Request'],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-557 @Ignore # Response code mismatch, expected: 400, actual: 500"
Scenario: must return a negative response when request contains a malformed case type id

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed case type id],
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-596
Scenario: must retrieve workbasket input details for dynamic display successfully

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-200 OK],
      And the response [body contrains LAST_STATE_MODIFIED_DATE field as one of input fields],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
