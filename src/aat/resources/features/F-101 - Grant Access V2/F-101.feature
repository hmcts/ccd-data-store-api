@F-101
Feature: F-101: Grant access to case by updating User Roles

  Background:
    Given an appropriate test context as detailed in the test data source

@S-525
  Scenario: must return 204 when grant is successful for a user to a valid case ID
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [uses the first solicitor to call the api and the second one to be granted access to the case just created],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 204 return code],
    And the response has all other details as expected.

@S-531
  Scenario: must return 204 when revoke is successful for a user to a valid case ID
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    And another successful call [to grant access to a case] as in [S-531-Grant],
    When a request is prepared with appropriate values,
    And the request [uses the first solicitor to call the api and the second one to be granted access to the case just created],
    And it is submitted to call the [Revoke access to a case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 204 return code],
    And the response has all other details as expected.

@S-526 @Ignore @RDM-7546
  Scenario: must return 400 error response for user ID with null value
    Given a user with [an active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [uses null value for user id],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 400 return code],
    And the response has all other details as expected.

@S-527
  Scenario: must return 400 error response for invalid case role
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [uses a case role that is not valid],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [has the 400 return code],
    And the response has all other details as expected.

@S-528
  Scenario: must return 400 error response for invalid case id
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [uses a case id that is not valid],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [has the 400 return code],
    And the response has all other details as expected.

@S-529 @Ignore @RDM-7545
  Scenario: must return 404 error response for case not found
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [uses a case id that does not exists],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [has the 404 return code],
    And the response has all other details as expected.

@S-530 @Ignore # re-write as part of RDM-6847
  Scenario: must return 403 error response for invalid authentication
    Given a user with [an active solicitor profile in CCD],
    And a user with [another active solicitor profile in CCD],
    And a successful call [to create a token for case creation] as in [F101_GetToken],
    And another successful call [to create a full case for solicitor] as in [F101_Case_Data_Solicitor],
    When a request is prepared with appropriate values,
    And the request [has invalid auth token],
    And it is submitted to call the [Grant access to a case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [has the 403 return code],
    And the response has all other details as expected.
