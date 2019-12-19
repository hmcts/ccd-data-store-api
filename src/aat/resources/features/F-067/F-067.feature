@F-067
Feature: F-067: Retrieve a CaseView Event by case and event id for dynamic display

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-212
  Scenario: should retrieve case view when the case reference and case event exists
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as Caseworker1] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [to create a full case] as in [Befta_Default_Full_Case_Creation_Data]
    And another successful call [to get the details about case event for the case just created] as in [S-212_Get_Case_Data]
    When a request is prepared with appropriate values
    And the request [contains the reference of the case just created and the event id valid for that case]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains HTTP 200 Ok]
    And the response has all other details as expected

  @S-211 @Ignore #This is an invalid scenario with respect to this endpoint.
  Scenario: should retrieve case view history when the case reference exists

  @S-210
  Scenario: should get 404 when case reference does NOT exist
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation as Caseworker1] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [to create a full case] as in [Befta_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 404 Not Found]
    And the response has all other details as expected

  @S-209
  Scenario: should get 400 when case reference invalid
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 400 Bad Request]
    And the response has all other details as expected

  @S-207 @Ignore
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [includes a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-208 @Ignore
  Scenario: must return negative response when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorised access to the operation]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [includes a HTTP 403 Forbidden]
    And the response has all other details as expected

