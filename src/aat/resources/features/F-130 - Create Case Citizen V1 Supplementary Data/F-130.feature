#=====================================================
@F-130
Feature: F-130: Submit case creation as Citizen
#====================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-1269 # must create case successfully for correct supplementary data inputs
Scenario: must create case successfully and return positive response HTTP-201 for correct inputs

    Given a user with [an active profile in CCD],
      And a successful call [to create an event token] as in [F-130-Prerequisite],

     When a request is prepared with appropriate values,
      And the request [contains the event token just created as above],
      And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-201],
      And the response has all other details as expected.


  @S-1270
  Scenario: Must return the updated supplementary data values from Data store
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - who can create a case],
    And a case [C1, which has just been] created as in [F130_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Update Supplementary Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.
