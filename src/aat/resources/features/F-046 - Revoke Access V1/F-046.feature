#====================================
@F-046
Feature: F-046: Revoke access to case
#====================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-222 # RDM-6800 RAISED for case sensitivity on response for header values
Scenario: must return 204 if access is successfully revoked for a user on a case ID

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],
      And a call [to grant testUser access to the case] will get the expected response as in [F-046_Grant_Access],
      And a call [to verify testUser has been granted a case role for the case] will get the expected response as in [F-046_Verify_Granted_Case_Role_for_Case],

     When a request is prepared with appropriate values,
      And the request [contains a valid case id],
      And it is submitted to call the [Revoke access to case] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has a 204 no content code],
      And the response has all other details as expected,
      And a call [to verify testUser has been granted no case roles for the case] will get the expected response as in [F-046_Verify_Granted_No_Case_Roles_for_Case].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-223 # ACTUALLY returns a 404
Scenario: must return 400 if case id is invalid

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case id],
      And it is submitted to call the [Revoke access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has a 404 not found code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-224 # ACTUALLY returns a 403
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials in CCD],
      And it is submitted to call the [Revoke access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has a 401 Unauthorized code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-225 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorized access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials in CCD],
      And it is submitted to call the [Revoke access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has a 403 Forbidden code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
