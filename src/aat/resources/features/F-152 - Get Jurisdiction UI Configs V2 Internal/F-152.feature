#=====================================================================
@F-152
Feature: F-152: Retrieve Jurisdiction UI Config Information for Dynamic Display
#=====================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-152.1
Scenario: must retrieve jurisdiction UI config information for the jurisdictions successfully

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [retrieve jurisdiction ui config information for the jurisdictions] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-152.2 @Ignore # Response code mismatch, expected: 401, actual: 403
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [retrieve jurisdiction ui config information for the jurisdictions] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-401 Unauthorised],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
