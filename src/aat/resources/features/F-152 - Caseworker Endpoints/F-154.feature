#============================================
@F-154
Feature: F-154: Create a caseworker draft
#============================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

@S-154.1
Scenario: create a draft for a caseworker
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
      And it is submitted to call the [Create a caseworker draft] operation of [CCD Data Store]
    Then a positive response is received
      And the response has all other details as expected

@S-154.2
Scenario: reject a draft request with an unknown event id
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
      And the request [contains an unknown event id]
      And it is submitted to call the [Create a caseworker draft] operation of [CCD Data Store]
    Then a negative response is received
      And the response [contains a HTTP-422 Unprocessable Entity]
      And the response has all other details as expected

@S-154.3
Scenario: reject a draft request with a valid event id and malformed case data
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
      And the request [contains a valid event id and case data that is not an object]
      And it is submitted to call the [Create a caseworker draft] operation of [CCD Data Store]
    Then a negative response is received
      And the response [contains a HTTP-400 Bad Request]
      And the response has all other details as expected
