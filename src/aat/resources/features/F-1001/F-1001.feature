@F-1001
Feature: F-1001: Get authorised document id to the user

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1001
  Scenario: must receive a document id for a valid user who has READ permission
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD with READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains an HTTP 200 OK status code]
    And the response [contains the document id which was sent in the request]
    And the response has all other details as expected

  @S-1002
  Scenario: must receive an error response when user doesn't have READ permission
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD with no READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 404 status code]
    And the response has all other details as expected

  @S-1003
  Scenario: must receive an error response when document id is not exist
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [has the case id just created]
    And the request [has a document id which is not exist in the system]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 404 status code]
    And the response has all other details as expected

  @S-1004
  Scenario: must receive an error response when document id is not associated with case id
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [has the case id just created]
    And the request [has a document id which is not associated with case id]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 404 status code]
    And the response has all other details as expected

  @S-1005
  Scenario: must receive an error response when case id is not exist
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [has a case id which is not exist in the system]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 404 status code]
    And the response has all other details as expected

  @S-1006
  Scenario: must return 403 when request provides without valid authorisation
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD with READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains null or an invalid user token]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 403 Access Denied]
    And the response has all other details as expected

  @S-1007
  Scenario: must return 403 when request provides without valid serviceAuthorisation
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    Given a user with [an active profile in CCD with READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains null or an invalid s2s authorisation token]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 403 Access Denied]
    And the response has all other details as expected

  @S-1008
  Scenario: must return 401 when request provides expired authentication
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD with READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains an expired user authorisation token]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 401 Unauthorised]
    And the response has all other details as expected

  @S-1009
  Scenario: must return 415 when request provides content type is application/xml
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD with READ access to a document]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains content type is application/xml]
    And it is submitted to call the [get authorised document id] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP 415 Unsupported Media Type]
    And the response has all other details as expected
