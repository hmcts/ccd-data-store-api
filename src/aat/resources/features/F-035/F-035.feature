@F-035
Feature: F-035: Retrieve a case by id

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-159
  Scenario: should retrieve case when the case reference exists
    Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains the case reference of the case just created]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the details of the case just created, along with an HTTP-200 OK]
    And the response has all other details as expected

  @S-155 @Ignore # defect RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid user authorisation token]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-401 Unauthorised]
    And the response has all other details as expected

  @S-156
  Scenario: must return 404 when request provides authentic credentials without authorised access to the operation
    Given a case that has just been created as in [S-156_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a valid user authorisation token without access to the operation]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-404 Not Found]
    And the response has all other details as expected

  @S-157
  Scenario: should get 400 when case reference invalid
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case reference]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-400 Bad Request]
    And the response has all other details as expected

  @S-158 @Ignore # defect RDM-6665
  Scenario: should get 404 when case reference does not exist
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a case reference that does not exist]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-404 Not Found]
    And the response has all other details as expected

 @S-591
  Scenario: must return status 200 along with the case-view object successfully
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-035.01_GetToken]
    And a case that has just been created as in [S-035.01_Case]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains Last State Modified Date metadata field]
    And the response has all other details as expected
