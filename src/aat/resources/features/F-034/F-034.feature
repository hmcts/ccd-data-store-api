@F-034
Feature: F-034: Validate case data

  Background: Validate the case data
    Given an appropriate test context as detailed in the test data source

  @S-314
  Scenario: should validate when the case type and event exists
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains the case data of the case just created]
    And it is submitted to call the [Validate case data] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains  case just created, along with a HTTP 200 OK]
    And the response has all other details as expected


