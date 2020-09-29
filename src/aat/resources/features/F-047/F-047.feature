#===================================================
@F-047 #Find case ids to which an user has access to
Feature: F-047: Get case ids
#===================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-097
Scenario: must return 200 and a list of case ids a user has access to

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a successful call [to grant access on the case just created] as in [F-047_Grant_Access],

     When a request is prepared with appropriate values,
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains a list of case ids, along with an HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-098
Scenario: must return 200 and an empty list if no case is found

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an userId which doesn't have access to the case],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains an empty list of case ids, along with an HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-099 @Ignore # Response code mismatch, expected: 401, actual: 403 Will be fixed as a part of RDM-6628
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-401 Unauthorized],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-100 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide authorised access to the operation],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-570 @Ignore # Response code mismatch, expected: 400, actual: 500 /RDM-7085
Scenario: must return negative response HTTP-400 when request contains a malformed user ID

    Given a user with [an inactive profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed user ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-571 @Ignore # Response code mismatch, expected: 400, actual: 500 RDM-7085
Scenario: must return negative response HTTP-400 when request contains a malformed case type ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed case type ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-572 @Ignore # Response code mismatch, expected: 400, actual: 403 / RDM-7106
Scenario: must return negative response HTTP-400 when request contains a malformed jurisdiction ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed jurisdiction ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-573 @Ignore # Response code mismatch, expected: 400, actual: 403 / RDM-7106
Scenario: must return negative response HTTP-400 when request contains a non-existing jurisdiction ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing jurisdiction ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-574 @Ignore #Response code mismatch, expected: 404, actual: 200" / RDM-7066
Scenario: must return negative response HTTP-404 when request contains a non-existing case type ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case type ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-575
Scenario: must return negative response HTTP-403 when request contains a non-existing user ID

    Given a user with [an inactive profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing user ID],
      And it is submitted to call the [Get case ids] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
