#============================================================================
@F-131 @elasticsearch
Feature: F-131: Retrieve a Start Event Trigger by ID for Supplementary Data
#============================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-1311
Scenario: should retrieve trigger when the case and event exists

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Supplementary_Data],

     When a request is prepared with appropriate values,
      And it is submitted to call the [Retrieve a start event trigger by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [includes the event start trigger for the case just created, along with a HTTP 200 OK],
      And the response has all other details as expected.

