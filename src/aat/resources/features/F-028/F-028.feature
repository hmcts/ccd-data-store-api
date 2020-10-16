#==============================================================================
@F-028
Feature: F-028: Fetch an event trigger in the context of a case for Case Worker
#==============================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-044
Scenario: must return event trigger data successfully for valid pre-state conditions

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],

     When a request is prepared with appropriate values,
      And the request [is prepared with valid Jurisdiction, Case ID and User ID],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 200 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-045 @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-046 @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request provides authentic credentials without authorized access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [provides a dummy authorisation token to the operation],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-547
Scenario: must return appropriate negative response when case-reference does not exists

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a structurally valid but non-existing case-reference],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-548
Scenario: must return appropriate negative response when event-id does not exists

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains invalid event-id],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-549
Scenario: must return appropriate negative response when case-reference is non-numeric

      Given a user with [an active profile in CCD],

       When a request is prepared with appropriate values,
        And the request [contains a non-numeric case-reference number],
        And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

       Then a negative response is received,
        And the response [has the 400 return code],
        And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-550
Scenario: must return appropriate negative response when Jurisdiction is invalid

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid Jurisdiction id],
      And it is submitted to call the [Fetch an event trigger in the context of a case for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
