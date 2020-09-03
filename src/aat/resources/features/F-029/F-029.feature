#===================================================================
@F-029
Feature: F-029: Fetch an event trigger in the context of a case type
#===================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-049
Scenario: must return successfully the current case event data filtered by case type ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [is prepared with valid input parameters],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 200 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-050 @Ignore # re-write as part of RDM-6847
Scenario: must return appropriate negative response when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-051 @Ignore # re-write as part of RDM-6847
Scenario: must return appropriate negative response when request contains an unauthorized access

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an unauthorized access to the operation],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-052
Scenario: must return appropriate negative response when request contains a non-existing case-type

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case-type],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-053
Scenario: must return appropriate negative response when request contains a non-existing event-id

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing event-id],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-551
Scenario: must return appropriate negative response when request contains a non-existing Jurisdiction

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing Jurisdiction id],
      And it is submitted to call the [Fetch an event trigger in the context of a case type for Case Worker] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
