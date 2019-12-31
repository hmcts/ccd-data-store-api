@F-040
Feature: F-040: Get Case for Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-080
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation

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
