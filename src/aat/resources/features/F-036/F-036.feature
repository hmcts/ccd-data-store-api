#================================================
@F-036
Feature: F-036: Retrieve a trigger for case by ID
#================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-199
Scenario: must return 200 and a StartTriggerResource object when case id and event trigger id are provided

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-200
Scenario: must return 400 when case reference does not exists

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case id],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-201 @Ignore # re-write as part of RDM-6847
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials in CCD],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 403 Unauthorized],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-202 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials in CCD],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-203
Scenario: must return 404 when event trigger is not found

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid event trigger id],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-204
Scenario: must return 422 when case event has no pre states

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an event trigger id with NO pre state],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 422 Unprocessable Entity],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-205 @Ignore #Ignoring test whilst we determine how call back validation should work on GET cases
Scenario: must return 422 when case event has validation errors

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains validation errors],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 422 Unprocessable Entity],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-206 @Ignore #Ignoring test Returns 500 raised RDM-6891
Scenario: must return 422 when user role is missing

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [where the user has insufficient privilege],
      And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains HTTP 422 Unprocessable Entity],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
