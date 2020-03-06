@F-1001
Feature: F-1001: Get Document AM Data for a given Case ID and Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1001
  Scenario: must successfully receive a document am data with a list of permissions
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a user with [an active Solicitor profile having full permissions on the same case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the requested document am data]
    And the response has all other details as expected

  @S-1002
  Scenario: must receive an error response for an active Solicitor profile who does not have READ access
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a user with [an active Solicitor profile having full permissions on the same case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [has the case id just created where a document id is associated]
    And the request [contains active Solicitor profile who does not have READ access]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1003
  Scenario: must receive an error response for a non existing document id
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [has the case id just created but a document id which does not exist]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1004
  Scenario: must receive an error response when document id is not associated with case id
    Given a case that has just been created as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [has the case id just created but a document id which is not associated with that case id]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1005
  Scenario: must receive an error response for a non existing case id
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non existing case id]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all other details as expected

  @S-1006
  Scenario: must receive an error response for a malformed document ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a malformed document ID]
    And it is submitted to call the [Get Document AM Data] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  #Generic Scenarios for Security
  @S-1007
  Scenario: generic scenario for Unauthorized

  @S-1008
  Scenario: generic scenario for Forbidden

  #Generic Scenarios for media type
  @S-1009
  Scenario: generic scenario for Unsupported Media Type

