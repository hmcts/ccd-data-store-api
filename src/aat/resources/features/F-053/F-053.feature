@F-053
Feature: F-053: Submit case creation as Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-269 # must create case successfully for correct inputs
  Scenario: must create case successfully and return positive response HTTP-201 for correct inputs
    Given a user with [an active profile in CCD]
    And   a successful call [to create an event token] as in [F-053-Prerequisite]
    When  a request is prepared with appropriate values
    And   the request [contains the event token just created as above]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [code is HTTP-201]
    And   the response has all other details as expected


  @S-270 @Ignore # wrong scenario in Excel
  Scenario: must return 201 if event creation is successful for a citizen

  @S-271 @Ignore # Response code mismatch, expected: 401, actual: 403
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [does not provide valid authentication credentials]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-401]
    And   the response has all other details as expected

  @S-272 @Ignore # re-write as part of RDM-6847
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [provides authentic credentials without authorised access to the operation]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-273 @Ignore # Postponed.
  Scenario: must return 409 if case is altered outside of transaction


  @S-274 @Ignore # Postponed
  Scenario: must return 409 when case reference is not unique


  @S-275 @Ignore # Postponed
  Scenario: must return 422 if event trigger has failed


  @S-276 @Ignore # Postponed
  Scenario: must return 422 when process could not be started


  @S-267 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed case type ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed case type ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-268 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed jurisdiction ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed jurisdiction ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-552 @Ignore # Response code mismatch, expected: 400, actual: 500 RDM-7358
  Scenario: must return negative response HTTP-400 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing jurisdiction ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-553
  Scenario: must return negative response HTTP-404 when request contains a non-existing case type ID
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing case type ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-404]
    And   the response has all other details as expected

  @S-554
  Scenario: must return negative response HTTP-403 when request contains a non-existing user ID
    Given a user with [an inactive profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing user ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected

  @S-555 @Ignore # Response code mismatch, expected: 400, actual: 500
  Scenario: must return negative response HTTP-400 when request contains a malformed user ID
    Given a user with [an inactive profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed user ID]
    And   it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-400]
    And   the response has all other details as expected

  @S-578
  Scenario: must create and update successfully the respective fields with ACL permissions for a Citizen
    Given a user with [an active Citizen profile in CCD]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Citizen_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Citizen_Case_Creation_Data]
    And another successful call [to get an update event token for the case just created] as in [S-578-Prerequisite_Citizen_Token_For_Update_Case]
    When a request is prepared with appropriate values
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains updated values for DocumentField2, along with an HTTP-201 Created]
    And the response has all other details as expected
    And another successful call [to get an update event token for the case just created] as in [S-578-Prerequisite_Citizen_Token_For_Update_Case]
    And a call [to update the DocumentField4 of same case by Citizen who doesn't have privilege to update DocumentField4] will get the expected response as in [S-578_Later_Case_Update_By_Citizen]

