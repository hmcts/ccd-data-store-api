@F-067
Feature: F-067: Retrieve a CaseView Event by case and event id for dynamic display

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-212
  Scenario: should retrieve case view when the case reference and case event exists
    Given a user with [an active profile in CCD]
    Given a successful call [to create a token for case creation as Caseworker1] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [to create a full case as a Caseworker1] as in [Befta_Default_Full_Case_Creation_Data]
    And another successful call [to get the details about case event for the case just created] as in [S-212_Get_Case_Data]
    When a request is prepared with appropriate values
    And the request [contains the reference of the case just created and the event id valid for that case]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains HTTP 200 Ok]
    And the response has all other details as expected

  @S-210
  Scenario: should get 404 when case event does NOT exist
    Given a user with [an active profile in CCD]
    Given a successful call [to create a token for case creation as Caseworker1] as in [Befta_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [to create a full case as a Caseworker1] as in [Befta_Default_Full_Case_Creation_Data]
    #And another successful call [to get the details about case event for the case just created] as in [S-212_Get_Case_Data]
    When a request is prepared with appropriate values
    #And the request [contains the reference of the case just created and the event id valid for that case]
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 404 Not Found]
    And the response has all other details as expected

  @S-209 #Ideally it should return response code 404
  Scenario: should retrieve case view when the case reference and case event exists
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [Retrieve an event by case and event IDs for dynamic display] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 200 Ok]
    And the response has all other details as expected

