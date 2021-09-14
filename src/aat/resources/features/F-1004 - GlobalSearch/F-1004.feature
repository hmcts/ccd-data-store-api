@F-1004
Feature: F-1004: Global Search

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1004.1
  Scenario: Update the Data Store for 'case create'
    Given a user with [an active profile in CCD]
    And a new 'Global Search' component has been established
    When a case that has just been created as in [S-1004.01_Case]
    Then the response [contains a SearchCriteria]

