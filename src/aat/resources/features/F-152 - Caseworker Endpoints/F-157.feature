#=============================================
@F-157
Feature: F-157: Update a caseworker draft
#=============================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

@S-157.1
Scenario: update a caseworker draft
    Given a user with [an active profile in CCD]
      And a successful call [to create a caseworker draft] as in [S-154.1]
    When a request is prepared with appropriate values
      And it is submitted to call the [Update a caseworker draft] operation of [CCD Data Store]
    Then a positive response is received
      And the response has all other details as expected

@S-157.2
Scenario: reject a draft update request with an unknown event id
    Given a user with [an active profile in CCD]
      And a successful call [to create a caseworker draft] as in [S-154.1]
    When a request is prepared with appropriate values
      And the request [contains an unknown event id]
      And it is submitted to call the [Update a caseworker draft] operation of [CCD Data Store]
    Then a negative response is received
      And the response [contains a HTTP-422 Unprocessable Entity]
      And the response has all other details as expected
