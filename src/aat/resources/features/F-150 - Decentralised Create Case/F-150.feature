@F-150
Feature: F-150: Decentralised case creation via V2 API

  Background: Load test context
    Given an appropriate test context as detailed in the test data source

  @S-150.01
  Scenario: must return 201 when creating a decentralised case succeeds
    Given a successful call [to start decentralised case creation] as in [F-150_CreateCase_Token_Creation],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 201 Created],
    And the response has all other details as expected.
