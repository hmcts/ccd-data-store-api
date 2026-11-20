@F-070
Feature: F-070: Nested Collection DataType

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-070.1
  Scenario: Search inputs fill null classifications for collection children without changing existing classifications
    Given a user with [read access to the nested collection case type]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve search input details for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [fills missing collection classifications and preserves existing classifications]
    And the response has all other details as expected

  @S-070.2
  Scenario: Workbasket inputs fill null classifications for collection children without changing existing classifications
    Given a user with [read access to the nested collection case type]
    When a request is prepared with appropriate values
    And it is submitted to call the [retrieve workbasket input details for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [fills missing collection classifications and preserves existing classifications]
    And the response has all other details as expected
