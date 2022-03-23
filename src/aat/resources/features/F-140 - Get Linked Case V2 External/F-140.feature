#====================================
@F-140
Feature: F-140: Retrieve a case by case reference
#====================================

Background:
    Given an appropriate test context as detailed in the test data source
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.1
Scenario: should get 400 when case reference invalid

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case reference],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.2
Scenario: should get 404 when case reference does not exist

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a case reference that does not exist],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.3
Scenario: must return 404 when request provides authentic credentials without authorised access to the operation

    Given a case that has just been created as in [Unauthorised_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a valid user authorisation token without access to the operation],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.4
Scenario: should get 400 when case reference is valid but startRecordNumber is invalid

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],


     When a request is prepared with appropriate values,
      And the request [contains valid case reference but invalid startRecordNumber],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.5
Scenario: should get 400 when case reference is valid but maxReturnRecordCount is invalid

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],


     When a request is prepared with appropriate values,
      And the request [contains valid case reference but invalid maxReturnRecordCount],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-140.6
Scenario: should retrieve case when the case reference exists

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve a linked case by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains the details of the case just created, along with an HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
