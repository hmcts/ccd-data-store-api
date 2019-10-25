@Case
Feature: Get case by reference

  @GetCase
  Scenario: should retrieve when a case reference exists if caseworker has 'CRUD' access on CaseType
    Given A case is available in system
    When Send the request
    Then The case is returned


