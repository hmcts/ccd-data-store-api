@F-056
Feature: F-056: Submit event creation as a Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-284
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorized access to the operation]
    And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-285
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid authentication credentials]
    And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-286 @Ignore
#    Code says "409" when case reference is not unique however we do not provide a case reference
#    Scenario is when the case has been altered outside the transaction, as the endpoint doesnt allow for case ref to be passed in this scenario
  #  to be ignored for now and investigated further later on
  Scenario: must return 409 for a case that has been altered outside of transaction

  @S-287
  Scenario: must return 422 when event submission fails
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation]
    When a request is prepared with appropriate values
    And the request [contains the token just generated and invalid case creation data]
    And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 422 Unprocessable Entity]
    And the response has all other details as expected

  @S-288
  Scenario: must return 201 when start event creation process for appropriate inputs
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation]
    When a request is prepared with appropriate values
    And the request [contains a token created as in Citizen_Token_Creation_Data_For_Case_Creation]
    And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [includes the case detail for the updated case, along with a HTTP 200 OK]
    And the response has all other details as expected


