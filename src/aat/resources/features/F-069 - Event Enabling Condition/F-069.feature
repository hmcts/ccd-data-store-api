#=====================================================
@F-069
Feature: F-069: Event Enabling Condition
#=====================================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-069.1 #CCD-3152
  Scenario: Injected data matches the data from the Callback and event is displayed

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [F-069_CaseCreation],

    When a request is prepared with appropriate values,
    And the request [contains a case that has just been created as in F-069_CaseCreation],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200 OK],
    And the response [contains the event that matches the data from the Callback],
    And the response has all other details as expected.

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-069.2 #CCD-3152
  Scenario: Injected data doesn't match the data from the Callback and event is not displayed

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [F-069_CaseCreation],

    When a request is prepared with appropriate values,
    And the request [contains a case that has just been created as in F-069_CaseCreation],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains HTTP 200 OK],
    And the response [doesn't contain the event that doesn't match the data from the Callback],
    And the response has all other details as expected.
