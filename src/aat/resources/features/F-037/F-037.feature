@F-037
Feature: F-037: Submit event for an existing case (V2)

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-024
  Scenario: should create event successfully for an existing case
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a successful call [to get an event token for the case just created] as in [S-024-Prerequisite]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created as in Standard_Full_Case_Creation_Data]
    And the request [contains a token created as in S-024-Prerequisite]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a positive response is received
    And the response [contains the case detail for the updated case, along with a HTTP 200 OK]
    And the response has all other details as expected

  @S-022
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-023
  Scenario: must return negative response when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorised access to the operation]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-025
  Scenario: must return negative response when request contains an invalid case-reference
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid case-reference]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a negative response is received
    And the response [contains a HTTP 400 'Bad Request']
    And the response has all the details as expected

  @S-026 @Ignore #This scenario is returning 400 instead of expected 404, Need to raise defect JIRA
  Scenario: must return negative response when request contains a non-existing case-reference
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing case-reference]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a negative response is received
    And the response [contains a HTTP 404 'Not Found']
    And the response has all the details as expected

  @S-027
  Scenario: must return negative response when request contains a non-existing Event-Id
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing Event-Id]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store]
    Then a negative response is received
    And the response [contains a HTTP 404 'Not Found']
    And the response has all the details as expected
