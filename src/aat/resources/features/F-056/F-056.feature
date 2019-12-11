@F-056
Feature: F-056: Submit event creation as Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-284
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorized access to the operation]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 forbidden code]
    And the response has all other details as expected

  @S-285
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid authentication credentials]
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 forbidden code]
    And the response has all other details as expected

  @S-286
  Scenario: must return 409 for a case that has been altered outside of transaction
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values
#    And the request [does not provide a valid authentication credentials]
#    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
#    Then a negative response is received
#    And the response [has the 403 return code]
#    And the response has all other details as expected

  @S-287
  Scenario: must return 422 when event submission fails
#    Given a user with [an active profile in CCD]
#    When a request is prepared with appropriate values
#    And the request [does not provide an authorized access to the operation]
#    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
#    Then a negative response is received
#    And the response [has the 403 return code]
#    And the response has all other details as expected

  @S-288
  Scenario: must return 201 when start event creation process for appropriate inputs
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation]
    When a request is prepared with appropriate values
    And it is submitted to call the [Submit case creation as Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 201 code]
    And the response has all other details as expected
