#======================================================
@F-121
Feature: F-121: Order Complex fields in start event
#======================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-501
Scenario: must order fields

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 200 return code],
      And the response has all other details as expected.
