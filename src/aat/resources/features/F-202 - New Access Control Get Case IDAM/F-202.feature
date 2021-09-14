@F-202 @Ignore
Feature: get case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-202.5
  Scenario: User can not access the case when user has no case role access
    Given a user with [restricted access to create a case J1-CT1-02]
    And a user with [PUBLIC SC ORGANISATION role assignment without case role access]
    And a case that has just been created as in [F-201_CT1]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT1-02]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-202.6
  Scenario: User can not access the case when Case Role has insufficient SC for Case Type
    Given a user with [restricted access to create a case J1-CT1-02]
    And a user with [PUBLIC SC ORGANISATION role assignment has insufficient SC for Case Type]
    And a case that has just been created as in [F-201_CT1]
    And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment with insufficient SC] as in [GRANT_CASE_ROLE_ASSIGNMENT_INSUFFICIENT_SC]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT1-02]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-202.10
  Scenario: Only fields with SC less than or equal to that of the actor are returned
    Given a user with [restricted access to create a case J1-CT2-01]
    And a user with [PUBLIC SC ORGANISATION role assignment to view the case and only PUBLIC field F3]
    And a case that has just been created as in [F-201_CT2]
    And a successful call [to give user Solicitor1 a PRIVATE CASE role assignment to view the previously created case] as in [GRANT_CASE_ROLE_ASSIGNMENT]
    When a request is prepared with appropriate values
    And the request [attempts to get case J1-CT2-01]
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store]
    Then a positive response is received
    Then the response [contains the PUBLIC field F3 from the ORGANISATION role assignment]
    And the response has all other details as expected

