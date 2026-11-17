#=====================================================
@F-153
Feature: F-153: Get events for a caseworker case
#=====================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

@S-153.1
Scenario: get events for a caseworker case
    Given a user with [an active profile in CCD]
      And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
      And it is submitted to call the [Get events for a caseworker case] operation of [CCD Data Store]
    Then a positive response is received
      And the response has all other details as expected

@S-153.2
Scenario: reject a malformed case reference when retrieving events
    Given a user with [an active profile in CCD]
      And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
      And the request [contains a malformed case reference]
      And it is submitted to call the [Get events for a caseworker case] operation of [CCD Data Store]
    Then a negative response is received
      And the response [contains a HTTP-400 Bad Request]
      And the response has all other details as expected
