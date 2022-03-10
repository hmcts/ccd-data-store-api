@F-130 @crud
Feature: F-130: Create Case External API CRUD Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-130.1
  Scenario: User successfully submits case creation containing a field with no CaseField R Access does not see the field in response
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-130.1_Get_Event_Trigger]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a positive response is received
    And the response has all other details as expected
    And the response [does not contain the field with no R CRUD access]


  @S-130.2
  Scenario: User cannot submit case creation containing a field without CaseField C Access
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-130.1_Get_Event_Trigger]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected
    And the response [contains an error stating that the field cannot be found]

  @S-130.3
  Scenario:User cannot submit case creation without CaseType C Access
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-130.1_Get_Event_Trigger]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected
    And the response [contains an error stating that the case type cannot be found]

  @S-130.4
  Scenario: User cannot submit case creation without CaseEvent C Access
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-130.1_Get_Event_Trigger]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected
    And the response [contains an error stating that the case event cannot be found]


  @S-130.7
  Scenario: User submits case creation with no CaseType R Access does not return the case after successful case creation
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-130.1_Get_Event_Trigger]
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a positive response is received
    And the response has all other details as expected
    And the response [contains an null values for case details]
