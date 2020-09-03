#=======================================================================
@F-066
Feature: F-066: Retrieve a Start Event Trigger by ID for Dynamic Display
#=======================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-182
Scenario: should retrieve trigger when the case and event exists

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],

     When a request is prepared with appropriate values,
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [includes the event start trigger for the case just created, along with a HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-175 @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [includes a HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-176  @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request provides authentic credentials without authorised access

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide an authorised access to the operation],
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [includes a HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-177
Scenario: must return negative response when request contains an invalid case reference

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case reference],
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [includes a HTTP 400 'Bad Request'],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-178
Scenario: must return negative response when request contains a non-existing case reference

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case reference],
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [includes a HTTP 404 'Not Found'],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-180
Scenario: must return negative response when request contains an invalid event trigger

      Given a user with [an active profile in CCD],
        And a case that has just been created as in [Standard_Full_Case_Creation_Data],

       When a request is prepared with appropriate values,
        And the request [contains an invalid event trigger],
        And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

       Then a negative response is received,
        And the response [includes a HTTP 404 'Not Found'],
        And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


