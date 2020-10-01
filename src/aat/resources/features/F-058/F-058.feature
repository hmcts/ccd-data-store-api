#=============================================
@F-058
Feature: F-058: Start Case Creation as Citizen
#=============================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-241
Scenario: must start case creation process successfully for correct inputs

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-200],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-237 @Ignore # Response code mismatch, expected: 401, actual: 403
Scenario: must return 401 when request provides invalid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a dummy user id],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-401],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-237
Scenario: must return 403 when the request contains a dummy user id

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a dummy user id],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-238 @Ignore # Response code mismatch, expected: 403, actual: 200
Scenario: must return 403 when the request contains a jurisdiction id that the user is unauthorized to access

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a jurisdiction id that the user is unauthorized to access],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-239
Scenario: must return 403 when request contains a malformed user ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed user id],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-240 @Ignore # Response code mismatch, expected: 400, actual: 500
Scenario: must return 400 when request contains a malformed Case Type ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed Case Type ID],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-517 @Ignore # Response code mismatch, expected: 404, actual: 200
Scenario: must return 404 when request contains a non-existing jurisdiction ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing jurisdiction ID],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-518
Scenario: must return 404 when request contains a non-existing Case type ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing Case type ID],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-519
Scenario: must return 404 when request contains a non-existing Event ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing Event ID],
      And it is submitted to call the [start case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
