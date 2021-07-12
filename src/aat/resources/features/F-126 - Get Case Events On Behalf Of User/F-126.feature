@F-126
Feature: F-126: Get Case Events On Behalf Of User

    Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1261
  Scenario: must successfully update userid with on behalf of user and proxied by user details
    Given a user with [an active profile in CCD]
      And a case [C1, which has just been] created as in [F126_Case_Data_Create_C1]
      And a successful call [to get an event token for the case just created] as in [F126_Prerequisite_Citizen_Token_For_Update_Case]
      And a successful call [using on_behalf_of user] as in [F126_Update_Case_With_On_Behalf_Of_User]

     When a request is prepared with appropriate values
      And it is submitted to call the [get event for an existing case (V2)] operation of [CCD Data Store]

     Then a positive response is received
      And the response has all the details as expected
