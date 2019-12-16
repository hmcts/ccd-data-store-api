@F-063
Feature: F-063: Retrieve search input details for dynamic display

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-215
  Scenario: should retrieve search inputs
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a valid case type]
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the correct search inputs for the given case type, along with an HTTP 200 OK]
    And the response has all other details as expected

  @S-216 @Ignore # not relevant to this operation, should be included in F-064
  Scenario: should retrieve workbasket inputs
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a valid case type]
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the correct workbasket inputs for the given case type, along with an HTTP 200 OK]
    And the response has all other details as expected

  @S-213 @Ignore # expected 401 but actually got 403 (defect RDM-6628)
  Scenario: must return 401 when request does not provide valid authentication credentials
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid user authentication token]
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 401 Unauthorised]
    And the response has all other details as expected

  @S-214
  Scenario: must return 404 when request provides authentic credentials without authorized access to the operation
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a user authentication token that does not have read access to the case type]
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 404 Not Found]
    And the response has all other details as expected

  @S-XXX
  Scenario: must return 404 when case type does not exist
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a case type that does not exist]
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 404 Not Found]
    And the response has all other details as expected

