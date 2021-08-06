@F-1001
Feature: F-1001: Get Document AM Data for a given Case ID and Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1051
  Scenario: must successfully receive a document am data with a list of permissions
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type],
    And a user with [an active Solicitor profile having full permissions on the same case type]
    And a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation],
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [S-1051-Case_Creation_Main]
    When a request is prepared with appropriate values,
    And the request [has the case id just created where a document id is associated],
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains the requested document am data],
    And the response has all other details as expected

  @S-1052
  Scenario: must receive an error response for an active Solicitor profile who does not have READ access
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a user with [an active Solicitor profile having full permissions on the same case type]
    And a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [S-1052-Case_Creation_Main]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains active Solicitor profile who does not have READ access]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1053
  Scenario: must receive an error response for a non existing document id
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [S-1053-Case_Creation_Main]
    When a request is prepared with appropriate values
    And the request [has the case id just created but a document id which does not exist]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

#  @S-1018 @Ignore #Duplicate of S-1017. Not required any more.
#  Scenario: must receive an error response when document id is not associated with case id
#    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
#    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
#    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
#    When a request is prepared with appropriate values
#    And the request [has the case id just created but a document id which is not associated with that case id]
#    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
#    Then a negative response is received
#    And the response has all other details as expected

  @S-1054
  Scenario: must receive an error response for a non existing case id
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a successful call [to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values
    And the request [contains a non existing case id]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected


  @S-1055
  Scenario: must receive an error response for a malformed document ID
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    When a request is prepared with appropriate values
    And the request [contains a malformed document ID]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  #Generic Scenarios for Security
  @S-1056 @Ignore
  Scenario: generic scenario for Unauthorized

  @S-1057 @Ignore
  Scenario: generic scenario for Forbidden

  #Generic Scenarios for media type
  @S-1058 @Ignore
  Scenario: generic scenario for Unsupported Media Type
