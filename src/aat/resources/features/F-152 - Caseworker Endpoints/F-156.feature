#=============================================
@F-156
Feature: F-156: Delete a caseworker draft
#=============================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

@S-156.1
Scenario: delete a caseworker draft
    Given a user with [an active profile in CCD]
      And a successful call [to create a caseworker draft] as in [S-154.1]
    When a request is prepared with appropriate values
      And it is submitted to call the [Delete a caseworker draft] operation of [CCD Data Store]
    Then a positive response is received
      And the response has all other details as expected

@S-156.2
Scenario: reject a draft delete request with a malformed draft id
    Given a user with [an active profile in CCD]
      And a successful call [to create a caseworker draft] as in [S-154.1]
    When a request is prepared with appropriate values
      And the request [contains a malformed draft id]
      And it is submitted to call the [Delete a caseworker draft] operation of [CCD Data Store]
    Then a negative response is received
      And the response [contains a HTTP-404 Not Found]
      And the response has all other details as expected
