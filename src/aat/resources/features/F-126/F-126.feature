@F-126 @currentrun
Feature: F-126: On Behalf Of User and Proxied by User on event creation

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1261
  Scenario: must successfully update userid with on behalf of user and proxied by user details
    Given an appropriate test context as detailed in the test data source,
    And a user with [an active profile in CCD]
    And a case [C1, which has just been] created as in [F126_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Post Event Data] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response has all the details as expected.
    And a call [to verify userid and proxied by user] will get the expected response as in [S-105.8_Get_Case_Roles_for_Case_C1].


