#===================================
@F-045
Feature: F-045: Grant access to case
#===================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-151
Scenario: must return 201 if the grant is successful for a user to a valid case ID

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a call [to verify testUser has been granted no case roles for the case] will get the expected response as in [F-045_Verify_Granted_No_Case_Roles_for_Case],

     When a request is prepared with appropriate values,
      And the request [uses the id of the case just created],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 201 return code],
      And the response has all other details as expected,
      And a call [to verify testUser has been granted a case role for the case] will get the expected response as in [F-045_Verify_Granted_Case_Role_for_Case].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-152
Scenario: must return 404 when case id is structurally valid but not exist in CCD

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains in input parameters a structurally valid but non-existing case-reference],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-153 @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide valid authentication credentials],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-154 @Ignore # re-write as part of RDM-6847
Scenario: must return negative response when request does not provide an authorized access

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide authorised access to the operation],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-544
Scenario: must return negative response when request body doesn't provide a mandatory field

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide a mandatory field for the operation],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 400 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-545 @Ignore
Scenario: must return negative response when case id contains some non-numeric characters

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide the numeric case id for the operation],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [has the 400 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-045.07
Scenario: must grant without removing other case roles

    Given a user with [an active profile in CCD],
      And a user [testUser - with an active profile in CCD],
      And a case that has just been created as in [F-045_Befta_Jurisdiction2_Full_Case_Creation_Data],
      And a call [to grant an extra case-role to the case] will get the expected response as in [F-045_Grant_Case_Role],
      And a call [to verify testUser has been granted the extra case role for the case] will get the expected response as in [F-045_Verify_Granted_Extra_Case_Role_for_Case],

     When a request is prepared with appropriate values,
      And the request [uses the id of the case just created],
      And it is submitted to call the [Grant access to case] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 201 return code],
      And the response has all other details as expected,
      And a call [to verify testUser has been granted multiple case roles for the case] will get the expected response as in [F-045_Verify_Granted_Multiple_Case_Roles_for_Case].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
