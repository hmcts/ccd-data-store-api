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

