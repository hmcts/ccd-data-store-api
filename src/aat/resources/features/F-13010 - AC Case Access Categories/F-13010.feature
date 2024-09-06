@F-13010 @currentrun
Feature: F-13010: Case Access Category Tests

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source


  @S-13010.1
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains category value starting with defined pattern, Access is granted.
    Given a case that has just been created as in [F-13010.1_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern Civil Standard, Criminal Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as Civil Standard Legal] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

  @S-13010.2
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains category value NOT starting with defined pattern, Access is not Granted
    Given a case that has just been created as in [F-13010.2_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern - Civil/Standard, Criminal/Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.


  @S-13010.3
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains NO category value, Access is NOT granted
    Given a case that has just been created as in [F-13010_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern - Civil/Standard, Criminal/Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as NULL] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.

  @S-13010.4
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains NO category, case contains category value, Access is GRANTED
    Given a case that has just been created as in [F-13010.3_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with NULL value] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected


  @S-13010.5
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfiles contains multiple categories plus at least one which has NO category, case contains category value NOT matching the start of any of those, Access is Granted (because the AccessProfile with NO category overrides)
    Given a case that has just been created as in [F-13010.4_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with NULL value] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with Civil/Standard, Criminal/Standard value] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

  @S-13010.9.1
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains category value starting with defined pattern, Access is granted.
    Given a case that has just been created as in [F-13010.1_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern - Civil/Standard, Criminal/Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as Civil/Standard/Legal] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

  @S-13010.9.2
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains category value NOT starting with defined pattern, Access is not Granted
    Given a case that has just been created as in [F-13010.2_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern - Civil/Standard, Criminal/Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.


  @S-13010.9.3
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains category, case contains NO category value, Access is NOT granted
    Given a case that has just been created as in [F-13010_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with pattern - Civil/Standard, Criminal/Serious] in the context,
    And [the case C1 contains an CaseAccessCategory field value as NULL] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a negative response is received
    And the response has all other details as expected.

  @S-13010.9.4
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfile contains NO category, case contains category value, Access is GRANTED
    Given a case that has just been created as in [F-13010.3_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with NULL value] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected

  @S-13010.9.5
  Scenario: RoleToAccessProfiles mapping for user’s derived AccessProfiles contains multiple categories plus at least one which has NO category, case contains category value NOT matching the start of any of those, Access is Granted (because the AccessProfile with NO category overrides)
    Given a case that has just been created as in [F-13010.4_CreateCase],
    And a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And [Role Assignments that don't apply for the scenario of creating a case have been filtered out] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with NULL value] in the context,
    And [the RoleToAccessProfiles tab contains CaseAccessCategory with Civil/Standard, Criminal/Standard value] in the context,
    And [the case C1 contains an CaseAccessCategory field value as FamilyLaw/Standard] in the context,
    And it is submitted to call the [external get case] operation of [CCD Data Store],
    Then a positive response is received,
    And the response has all the details as expected
