@F-1007 @elasticsearch
Feature: F-1007: GlobalSearch - AC Case Access Categories

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a successful call [to create the global search index] as in [GlobalSearchIndexCreation]

  @S-1007.1 @AC1
  Scenario: Successfully search for case with the new global search parameters and case access category value starting with defined CaseAccessCategories pattern in the RolesToAccessProfiles tab, case should be returned
    Given a user with [CaseAccessCategory with pattern - "Civil/Standard, Criminal/Serious"]
    And a successful call [to create a case] as in [F-1007_CreateCase_Civil/Standard/Legal]
    When a request is prepared with appropriate values
    And the request [contains a case with CaseAccessCategory field value as Civil/Standard/Legal]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns the case with CaseAccessCategory field value as Civil/Standard/Legal],
    And the response has all other details as expected.

  @S-1007.2  @AC2
  Scenario: Unsuccessful search for case with the new global search parameters, case contains category value NOT starting with defined CaseAccessCategories pattern, case should NOT be returned
    Given a user with [CaseAccessCategory with pattern - "Civil/Standard/Legal, Civil/Standard/Claim"]
    And a successful call [to create a case] as in [F-1007_CreateCase_FamilyLaw/Standard]
    When a request is prepared with appropriate values
    And the request [contains a case with CaseAccessCategory field value as FamilyLaw/Standard]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns 0 cases],
    And the response has all other details as expected.

  @S-1007.3  @AC3
  Scenario:  Unsuccessful search, Case contains NO category value but RolesToAccessProfiles contains CaseAccessCategories, case should NOT be returned when Global Search invoked for cases
    Given a user with [CaseAccessCategory with pattern - "Civil/Standard"]
    And a successful call [to create a case] as in [F-1007_CreateCase_NoCaseAccessCategory]
    When a request is prepared with appropriate values
    And the request [contains a case with CaseAccessCategory field value as null]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns 0 cases],
    And the response has all other details as expected.

  @S-1007.4  @AC4
  Scenario:  Successfully search for case with the new global search parameters and case access category for case has non-null value, CaseAccessCategories pattern in the RolesToAccessProfiles tab is NULL, case should be returned
    Given a user with [CaseAccessCategory with pattern - NULL]
    And a successful call [to create a case] as in [F-1007_CreateCase_NoCaseAccessCategory]
    When a request is prepared with appropriate values
    And the request [contains a case with CaseAccessCategory field value as null]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns the case with CaseAccessCategory field value as NULL],
    And the response has all other details as expected.

  @S-1007.5  @AC5
  Scenario:  RoleToAccessProfiles mapping for user’s derived AccessProfiles contains multiple categories plus at least one which has NO category, case contains category value NOT matching the start of any of those, case should be returned (because the AccessProfile with NO category overrides)
    Given a user with [CaseAccessCategory with pattern - NULL]
    Given a user with [CaseAccessCategory with pattern "Civil/Standard, Criminal/Standard"]
    And a successful call [to create a case] as in [F-1007_CreateCase_FamilyLaw/Standard]
    When a request is prepared with appropriate values
    And the request [contains a case with CaseAccessCategory field value as null]
    And the request [contains all the mandatory parameters]
    And it is submitted to call the [Global Search] operation of [CCD Data Store]
    Then a positive response is received,
    And the response [has 200 return code],
    And the response [returns the case with CaseAccessCategory field value as NULL],
    And the response has all other details as expected.
