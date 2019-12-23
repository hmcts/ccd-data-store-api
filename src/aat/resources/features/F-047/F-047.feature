@F-047 #Find case ids to which an user has grant access
Feature: F-047: Get case ids

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-097
  Scenario: must return 200 and a list of case ids a user has access to
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a successful call [to grant access on a case] as in [F-047_Grant_Access]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get case ids] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a list of case ids, along with an HTTP-200 OK]
    And the response has all other details as expected

  @S-098
  Scenario: must return 200 and an empty list if no case is found
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [contains an userId which doesn't have access to the case]
    And it is submitted to call the [Get case ids] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains an empty list of case ids, along with an HTTP-200 OK]
    And the response has all other details as expected

  @S-099 # This endpoint is returning 403. Will be fixed as a part of RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Get case ids] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-403 Forbidden]
    And the response has all other details as expected

  @S-100
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Get case ids] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-403 Forbidden]
    And the response has all other details as expected

