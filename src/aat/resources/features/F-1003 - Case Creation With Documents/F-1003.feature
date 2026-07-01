@F-1003
Feature: F-1003: Submit Case Creation

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1030
  Scenario: must successfully create a case with new document uploaded
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains necessary details about the document attached to the case],
    And   the response has all other details as expected,
    And   a call [to retrieve case details by case id] will get the expected response as in [Default_Get_Case_Data_Base].

  @S-1031
  Scenario: must successfully create a case with multiple documents uploaded
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_01],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_02],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_03],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_04],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_05],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_06],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains necessary details about the document attached to the case],
    And   the response has all other details as expected,
    And   a call [to retrieve case details by case id] will get the expected response as in [Default_Get_Case_Data_Base_01].

  @S-1032.1
  Scenario: must get an error response for a malformed case type ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a malformed case type ID]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1032.2
  Scenario: must get an error response for a case type ID containing potentially malicious characters
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a case type ID containing potentially malicious characters]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1033
  Scenario: must get an error response for a non-existing case type ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a non-existing case type ID]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1035 @Ignore # Defect AM-708
  Scenario: must get an error response for a malformed Hashtoken
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a malformed Hashtoken]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1036 @Ignore # Defect AM-708
  Scenario: must get an error response for a non-existing document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a non-existing document ID]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1037
  Scenario: must get an error response for a malformed document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case],
    And   the request [contains a malformed document ID]
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-1038 @Ignore 
  Scenario: generic scenario for Unauthorized

  @S-1039 @Ignore 
  Scenario: generic scenario for Forbidden

  @S-1040 @Ignore 
  Scenario: generic scenario for Unsupported Media Type

  @S-1041
  Scenario: Submit case creation event without any documents but callback adds a document to the case_data
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_07],
    And   a successful call [to register a document info callback stub with the ccd-test-stubs service] as in [Register_Doc_Info_Stub],
    And   a successful call [to create a token for case creation] as in [S-1041_Case_Data_Create_Token_Creation],
    When  a request is prepared with appropriate values,
    And   the request [does not contain a document],
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains a document attached to the case],
    And   the response has all other details as expected.

  @S-1042
  Scenario: must successfully create a case with new document uploaded where binary_url is not specified
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case without specifying binary_url],
    And   it is submitted to call the [Submit Case Creation With Doc Fields] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains necessary details about the document attached to the case],
    And   the response has all other details as expected,
    And   a call [to retrieve case details by case id] will get the expected response as in [Default_Get_Case_Data_Base_02]

  @S-1043
  Scenario: must successfully create a case with new document uploaded where filename is not specified
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    When  a request is prepared with appropriate values,
    And   the request [is to attach the document uploaded above to a new case without specifying filename],
    And   it is submitted to call the [Submit Case Creation With Doc Fields] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains necessary details about the document attached to the case],
    And   the response has all other details as expected,
    And   a call [to retrieve case details by case id] will get the expected response as in [Default_Get_Case_Data_Base_02]

  @S-1045 # CCD-7472 regression: documents stripped by the about-to-submit callback must not be attached to CDAM
  Scenario: Submit case creation with a document but the callback strips it - document must not be persisted
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_45],
    And   a successful call [to register a document strip callback stub with the ccd-test-stubs service] as in [Register_Doc_Strip_Stub],
    And   a successful call [to create a token for case creation] as in [S-1045_Case_Data_Create_Token_Creation],
    When  a request is prepared with appropriate values,
    And   the request [contains a document with a valid hash token],
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [does not contain the document stripped by the callback],
    And   the response has all other details as expected.

  @S-1046 # CCD-7472 guard: the saved-data filter must not drop documents the callback retains
  Scenario: Submit case creation with a document and the callback echoes the data - document must be persisted
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data_46],
    And   a successful call [to register a document echo callback stub with the ccd-test-stubs service] as in [Register_Doc_Echo_Stub],
    And   a successful call [to create a token for case creation] as in [S-1046_Case_Data_Create_Token_Creation],
    When  a request is prepared with appropriate values,
    And   the request [contains a document with a valid hash token],
    And   it is submitted to call the [Submit Case Creation] operation of [CCD Data Store],
    Then  a positive response is received,
    And   the response [contains the document attached to the case],
    And   the response has all other details as expected.
