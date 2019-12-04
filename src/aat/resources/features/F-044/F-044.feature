@F-044
Feature: F-044: Submit event creation as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-278
  Scenario: must submit the event creation successfully for correct inputs
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And a case that has just been created as in [Standard_Full_Case]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 201 return code]
    And the response has all other details as expected

  @S-279 @Ignore
  Scenario: must return negative response when request does not provide a valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-280 @Ignore
  Scenario: must return negative response when request does not provide an authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-281 @Ignore
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing jurisdiction ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all the details as expected

  @S-282 @Ignore
  Scenario: must return 404 when request contains a non-existing Case type ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing Case type ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

  @S-283 @Ignore
  Scenario: must return 404 when request contains a non-existing Event ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing Event ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

  @S-552 @Ignore
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing jurisdiction ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

