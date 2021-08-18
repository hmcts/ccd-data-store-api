@F-201 @ra
Feature: get case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-201.4
    Scenario: User can get a case only seeing fields which they have SC access to
      Given a user with [restricted access to create a case J1-CT2-01]
      And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
      And a case that has just been created as in [F-201_CT2]
      And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case J1-CT2-02] as in [GRANT_CASE_ROLE_ASSIGNMENT]
      When a request is prepared with appropriate values
      And the request [attempts to get case J1-CT2-02]
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
      Then a positive response is received
      Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
      Then the response [contains the PRIVATE field F2 from the CASE role assignment]
      And the response has all other details as expected


