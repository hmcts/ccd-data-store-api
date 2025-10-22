@F-150
Feature: F-150: Decentralised case creation via V2 API

  Background: Load test context
    Given an appropriate test context as detailed in the test data source

  @S-150.01 @Ignore
  Scenario: must return 201 when creating a decentralised case succeeds
    Given a successful call [to start decentralised case creation] as in [F-150_CreateCase_Token_Creation],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 201 Created],
    And the response has all other details as expected.

  @S-150.07 @Ignore
  Scenario: must return 409 when a decentralised case create conflicts
    Given a successful call [to start decentralised case creation] as in [F-150_CreateCase_Conflict_Token_Creation],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains an HTTP 409 'Conflict'],
    And the response has all other details as expected.

  @S-150.08 @Ignore
  Scenario: must surface validation errors when decentralised case create is rejected
    Given a successful call [to start decentralised case creation] as in [F-150_CreateCase_ValidationError_Token_Creation],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [external create case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains an HTTP 422 'Unprocessable Entity'],
    And the response has all other details as expected.

  @S-150.02 @Ignore
  Scenario: must return 200 when retrieving a decentralised case after creation
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains the case reference of the case just created],
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains the details of the case just created, along with an HTTP-200 OK],
    And the response has all other details as expected.
 
  @S-150.03 @Ignore
  Scenario: must return audit history when retrieving decentralised case events
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains the case reference of the case just created],
    And it is submitted to call the [Retrieve audit events by case ID] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains all audit event details under the case],
    And the response has all other details as expected.

  @S-150.04 @Ignore
  Scenario: must return internal case view for decentralised case
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains the case reference of the case just created],
    And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok],
    And the response has all other details as expected.

  @S-150.05 @Ignore
  Scenario: must return decentralised event history when retrieving a specific event by id
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains the case reference and event id obtained from the case view],
    And it is submitted to call the [Retrieve a CaseView Event by case and event id for dynamic display] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains HTTP 200 Ok],
    And the response has all other details as expected.

  @S-150.06 @Ignore
  Scenario: must add stub value when updating decentralised supplementary data
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains the case reference of the case just created],
    And it is submitted to call the [Update Supplementary Data] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all other details as expected.
