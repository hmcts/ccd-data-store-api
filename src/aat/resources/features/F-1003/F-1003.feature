@F-1003
Feature: F-1003: Submit Case Creation (V2.1)

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1030
  Scenario: must successfully create a case with new document uploded
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
    When a request is prepared with appropriate values,
    And the request [is to attach the document uploaded above to a new case],
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains necessary details about the document attached to the case],
    And the response has all other details as expected,
    And a call [to get the same case from data store] will get the expected response as in [GET_CASE_DETAILS].

  @S-1031
  Scenario: must successfully create a case with multiple documents uploded
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_1],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_2],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_3],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_4],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_5],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_6],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_7],
    When a request is prepared with appropriate values,
    And the request [is to attach the documents uploaded above to a new case],
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains necessary details about the document attached to the case],
    And the response has all other details as expected,
    And a call [to get the same case from data store] will get the expected response as in [GET_CASE_DETAILS].

  @S-1032
  Scenario: must get an error response for a malformed case type ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a malformed case type ID]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1033
  Scenario: must get an error response for a non-existing case type ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a non-existing case type ID]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1034
  Scenario: must get an error response for a non-existing Hashtoken
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a non-existing Hashtoken]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1035
  Scenario: must get an error response for a malformed Hashtoken
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a malformed Hashtoken]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1036
  Scenario: must get an error response for a non-existing document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a non-existing document ID]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1037
  Scenario: must get an error response for a malformed document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [is to attach the document uploaded above to a new case],
    And the request [contains a malformed document ID]
    And it is submitted to call the [Submit Case Creation (V2.1)] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-1038
  Scenario: generic scenario for Unauthorized

  @S-1039
  Scenario: generic scenario for Forbidden

  @S-1040
  Scenario: generic scenario for Unsupported Media Type
