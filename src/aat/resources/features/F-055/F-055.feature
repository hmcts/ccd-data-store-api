#==============================================
@F-055
Feature: F-055: Start event creation as Citizen
#==============================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-251 @Smoke
Scenario: must return 200 if start event trigger is successful for a case

    Given a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [includes case data matching the case just created],
      And it is submitted to call the [start event creation as citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains an event token for the triggered event, along with a HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-252 @Ignore # expected 401 but got 403 (defect RDM-6628)
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [includes an invalid user authorisation token],
      And it is submitted to call the [start event creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 401 Unauthorised],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-253 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

    Given a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [includes an valid user authorisation token that does not have access to the operation],
      And it is submitted to call the [start event creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-254 @Ignore # expected 404 but got 400 (defect RDM-6665)
Scenario: must return 404 if case is not found

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [includes an invalid case id],
      And it is submitted to call the [start event creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-255
Scenario: must return 422 when start event trigger has failed

    Given a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [includes unprocessable case data],
      And it is submitted to call the [start event creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 422 Unprocessable Entity],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
