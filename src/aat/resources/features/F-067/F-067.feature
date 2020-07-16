@F-067
Feature: F-067: Retrieve a CaseView Event by case and event id for dynamic display

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-212
  Scenario: should retrieve case view with response code HTTP 200 when the case reference and case event exists
    Given a user with [an active profile in CCD]
    And   a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And   another successful call [to create a full case] as in [Befta_Case_Data_Extension]
    And   another successful call [to get the details about case event for the case just created above] as in [S-212_Get_Case_Data]
    When  a request is prepared with appropriate values
    And   the request [contains the reference of the case just created and the event id valid for that case]
    And   it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [contains HTTP 200 Ok]
    And   the response has all other details as expected

  @S-211 @Ignore #This is an invalid scenario with respect to this endpoint.
  Scenario: should retrieve case view history when the case reference exists

  @S-207 @Ignore # re-write as part of RDM-6847
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [does not provide valid authentication credentials]
    And   it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [includes a HTTP 403 Forbidden]
    And   the response has all other details as expected

  @S-208 @Ignore # re-write as part of RDM-6847
  Scenario: must return negative response when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [does not provide an authorised access to the operation]
    And   it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [includes a HTTP 403 Forbidden]
    And   the response has all other details as expected

  @S-209
  Scenario: should get 400 when request contains non-existing case reference
    Given a user with [an active profile in CCD]
    And   a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And   another successful call [to create a full case] as in [Befta_Case_Data_Extension]
    And   another successful call [to get the details about case event for the case just created above] as in [S-212_Get_Case_Data]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing case reference]
    And   it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [contains HTTP 400 Bad Request]
    And   the response has all other details as expected

  @S-210  @Ignore # Response code mismatch, expected: 400, actual: 500 /RDM-7085
  Scenario: should get 400 when request contains non-existing event Id
    Given a user with [an active profile in CCD]
    And   a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And   another successful call [to create a full case] as in [Befta_Case_Data_Extension]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing event Id]
    And   it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [contains HTTP 400 Not Found]
    And   the response has all other details as expected
