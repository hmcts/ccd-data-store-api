@F-1003
Feature: F-1003: Upload document while case creation operation for V2.1

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1030
  Scenario: must successfully Upload document while case creation operation
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

  @S-1031
  Scenario: must successfully Upload multiple documents while case creation operation
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains multiple documents]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

  @S-1032
  Scenario: must get an error response contains a malformed case type Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed case type Id]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1033
  Scenario: must get an error response contains a non-existing case type Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing case type Id]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1034
  Scenario: must get an error response contains a non-existing Hashtoken
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing Hashtoken]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1035
  Scenario: must get an error response contains a malformed Hashtoken
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed Hashtoken]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1036
  Scenario: must get an error response contains a non-existing document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing document ID]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1037
  Scenario: must get an error response contains a malformed document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed document ID]
    And   it is submitted to call the [Upload document while case creation operation] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1038
  Scenario: generic scenario for Unauthorized

  @S-1039
  Scenario: generic scenario for Forbidden

  @S-1040
  Scenario: generic scenario for Unsupported Media Type
