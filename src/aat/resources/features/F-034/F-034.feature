@F-034
Feature: F-034: Validate case data

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-314
  Scenario: must return 200 when the case type and event exists
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains the case type and event]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the case type and event, along with a HTTP 200 OK]
    And the response has all other details as expected

  @S-308 @Ignore #This scenario is return 403 instead of 401. Jira: RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid user authentication token]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 401 Unauthorised]
    And the response has all other details as expected

  @S-309 @Ignore # re-write as part of RDM-6847
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid authentication credentials]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-310
  Scenario: must return 404 when case type does not exist
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing case type id]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 404 Not Found]
    And the response has all other details as expected

  @S-311 @Ignore #This scenario is return 500 instead of 404. Jira: RDM-7084
  Scenario: must return 404 when event not provided
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid event id]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 404 Not Found]
    And the response ha s all other details as expected

  @S-312 @Ignore #This scenario is invalid. Jira: RDM-6410
  Scenario: must return 422 when event trigger does not exist

