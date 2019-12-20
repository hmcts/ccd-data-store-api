@F-054
Feature: F-054: Get case for Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-093 # must return 200 and ist of case data for the given case id
  Scenario: must return 200 and  of case data for the given case id
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Citizen_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [contains a case Id that has just been created above]
    And it is submitted to call the [get case for citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [code is HTTP-200]
    And the response has all the details as expected
