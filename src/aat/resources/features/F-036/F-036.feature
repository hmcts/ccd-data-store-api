@F-036
Feature: F-036: CCD Data Store Api :: Retrieve a trigger for case by ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#  @S-198 <More tests out of further analysis>

  @S-199 must return 200 and a StartTriggerResource object when case id and event trigger id are provided
    Given a case has been created as in [Standard-Full-Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has a 200 code]
    And the response has all other details as expected

  @S-200 must return 400 when casereference does not exists
    Given a case has been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case id]
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 400 bad request code]
    And the response has all other details as expected

  @S-201 must return 401 when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials in CCD]
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 401 Unauthorized code]
    And the response has all other details as expected

  @S-202 must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials in CCD]
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 403 Forbidden code]
    And the response has all other details as expected

  @S-203 must return 404 when event trigger is not found
    Given a case has been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid event trigger id]
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 404 not found code]
    And the response has all other details as expected

  @S-204 must return 422 when case event has no pre states
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 422 Unprocessable Entity code]
    And the response has all other details as expected

  @S-205 must return 422 when case event has validation errors
    Given a case has been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains validation errors]
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 422 Unprocessable Entity code]
    And the response has all other details as expected

  @S-206 must return 422 when user role is missing
    Given a case has been created as in [Standard_Full_Case]
    And a user with [an invalid role]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve a trigger for case by ID] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 422 Unprocessable Entity code]
    And the response has all other details as expected 
