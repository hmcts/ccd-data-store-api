@F-040
Feature: F-040: Get Case for Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-576
  Scenario: must return successfully all and only the respective fields with READ access for a Case Worker and a Solicitor
    Given a user with [an active Case Worker profile in CCD with a specific variation of ACLs on a case type]
    And a user with [an active solicitor profile with another specific variation of ACLs on the same case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [is prepared on behalf of the user creating the case above]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains values for all fields under the case type]
    And the response has all other details as expected
    And a call [to get the same case by the Solicitor] will get the expected response as in [F-040_Later_Case_Read_By_Solicitor]
    And a call [to get the same case by the Case Worker] will get the expected response as in [F-040_Later_Case_Read_By_Case_Worker]

  @S-577 @Ignore @WIP
  Scenario: must return appropriate negative response for a jurisdiction id not existing in CCD

  @S-578 @Ignore @WIP
  Scenario: must return appropriate negative response for a case type id not existing in CCD

  @S-579 @Ignore @WIP
  Scenario: must return appropriate negative response for a case id not existing in CCD

  @S-580 @Ignore @WIP
  Scenario: must return a negative response for a request without a Bearer token
    Given a user with [an active case worker profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not contain a Bearer token]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all other details as expected

  @S-581 @Ignore @WIP
  Scenario: must return a negative response for a request with a dummy Bearer token
    Given a user with [an active case worker profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a dummy Bearer token]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all other details as expected

  @S-582 @Ignore @WIP
  Scenario: must return a negative response for a request with an expired Bearer token
    Given a user with [an active case worker profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an expired Bearer token]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all other details as expected

  @S-583 @Ignore @WIP
  Scenario: must return a negative response for a request with a Bearer token issued to the name of an unrecognised user
    Given a user with [an active case worker profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a Bearer token issued to the name of an unrecognised user]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all other details as expected

  @S-080 @Ignored @Ignore @WIP
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation

  @S-081 @Ignore
  Scenario: should get 400 when case reference invalid
  <already implemented previously. will be refactored later.>

  @S-082 @Ignore
  Scenario: should get 404 when case reference does NOT exist
  <already implemented previously. will be refactored later.>

  @S-083 @Ignore
  Scenario: should not retrieve when a case reference exists if caseworker has 'C' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-084 @Ignore
  Scenario: should not retrieve when a case reference exists if caseworker has 'CU' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-085 @Ignore
  Scenario: should not retrieve when a case reference exists if caseworker has 'D' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-086 @Ignore
  Scenario: should not retrieve when a case reference exists if caseworker has 'U' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-087 @Ignore
  Scenario: should retrieve case when the case reference exists
  <already implemented previously. will be refactored later.>

  @S-088 @Ignore
  Scenario: should retrieve when a case reference exists if caseworker has 'CR' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-089 @Ignore
  Scenario: should retrieve when a case reference exists if caseworker has 'CRUD' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-090 @Ignore
  Scenario: should retrieve when a case reference exists if caseworker has 'R' access on CaseType
  <already implemented previously. will be refactored later.>

  @S-091 @Ignore
  Scenario: should retrieve when a case reference exists if caseworker has 'RU' access on CaseType
  <already implemented previously. will be refactored later.>
