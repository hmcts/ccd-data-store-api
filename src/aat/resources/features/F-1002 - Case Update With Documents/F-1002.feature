@F-1002
Feature: F-1002: Submit Event for an Existing Case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1011
  Scenario: must successfully update case data content with Id and Token for a new document
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1011-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains an Event Id received from upstream],
    And the request [contains a Case Id and Document Id created above],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all other details as expected,
    And a call [to retrieve case details by case id] will get the expected response as in [S-1011_Get_Case_Data_Base].

  @S-1012
  Scenario: must successfully update case data content without any document field
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1012-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains an Event Id received from upstream],
    And the request [contains a Case Id created above],
    And the request [is to update the case created above, with some updates in only non document fields],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all other details as expected,
    And a call [to retrieve case details by case id] will get the expected response as in [S-1012_Get_Case_Data_Base].

  @S-1013
  Scenario: must successfully update case data content for multiple documents
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_1],
    And a successful call [by same user to upload another document with mandatory metadata] as in [Default_Document_Upload_2],
    And a successful call [by same user to upload another document with mandatory metadata] as in [Default_Document_Upload_3],
    And another successful call [to get an update event token for the case just created] as in [S-1013-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains an Event Id received from upstream],
    And the request [contains the Case Id along with ids and hash tokens of the documents uploaded above],
    And the request [also contains updates in non document fields],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all other details as expected,
    And a call [to get the same case containing new documents from data store] will get the expected response as in [S-1013_Get_Case_Data_Base].

  @S-1014 # Defect AM-708
  Scenario: must get an error response for a wrong hash token without any change applied to Case
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1014-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains an Event Id received from upstream],
    And the request [is to update the case created above, with some updates in non document fields and the Document field],
    And the request [contains a Document Id just created above, along with a wrong hash token for it],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to get the same case with unmodified contents from data store] will get the expected response as in [S-1014_Get_Case_Data_Base].

  @S-1015 # Defect AM-708
  Scenario: must get an error response for a non existing document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1015-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains a non existing document Id],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-1016
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1016-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains a malformed document Id],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-1017
  Scenario: must get an error response for a non existing Case Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1017-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains a non existing Case Id],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-1018
  Scenario: must get an error response for a malformed Case Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by same user to create a case of this case type] as in [F-1002-Case_Creation_Data],
    And another successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to get an update event token for the case just created] as in [S-1018-Update_Event_Token_Creation],
    When a request is prepared with appropriate values,
    And the request [contains a malformed Case Id],
    And it is submitted to call the [Submit Event for an Existing Case] operation of [CCD Data Store],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-1019
  Scenario: generic scenario for Unauthorised

  @S-1020
  Scenario: generic scenario for Forbidden

  @S-1021
  Scenario: generic scenario for Unsupported Media Type
