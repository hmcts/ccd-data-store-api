@F-000
Feature: [SAMPLE] Get default settings for user

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-151
  Scenario: must return 201 if the grant is successful for a user to a valid case ID
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [has a valid case-reference in input parameters]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 201 return code]
    And the response has all the details as expected
