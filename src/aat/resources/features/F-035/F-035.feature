@F-035
Feature: Retrieve a case by id

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-159
  Scenario: should retrieve case when the case reference exists
    Given a case has just been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a valid case reference]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the correct case with a 200 OK code]
    And the response has all other details as expected

  @S-155
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a case has just been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid user authorisation token]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 401 Unauthorized code]
    And the response has all other details as expected

  @S-156
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a case has just been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a valid user authorisation token without access to the operation]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 403 Forbidden code]
    And the response has all other details as expected

  @S-157
  Scenario: should get 400 when case reference invalid
    Given a case has just been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case reference]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 400 Bad Request code]
    And the response has all other details as expected

  @S-158
  Scenario: should get 404 when case reference does NOT exist
    Given a case has just been created as in [Standard_Full_Case]
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a case reference that does not exist]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has a 404 Not Found code]
    And the response has all other details as expected
