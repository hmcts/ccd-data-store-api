@F-1003-V2
Feature: F-1003-V2: Submit Case Creation (V2)

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1045
  Scenario: must successfully create a case with new document uploded
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   it is submitted to call the [Submit Case Creation (V2)] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains necessary details about the document attached to the case],
    And   the response has all other details as expected,
    And   a call [to retrieve case details by case id] will get the expected response as in [Default_Get_Case_Data_Base].

  @S-1046 @Ignore # Defect RDM-8403
  Scenario: must get an error response for a malformed case type ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a malformed case type ID]
    And   it is submitted to call the [Submit Case Creation (V2)] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

   @S-1053
  Scenario: generic scenario for Unauthorized

  @S-1054
  Scenario: generic scenario for Forbidden

  @S-1055
  Scenario: generic scenario for Unsupported Media Type
