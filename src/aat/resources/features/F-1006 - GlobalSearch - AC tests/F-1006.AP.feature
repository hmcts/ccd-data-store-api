@F-1006.AP @elasticsearch
Feature: F-1006: Global Search - Access Control Tests - AccessProcess

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source,

  @S-1006.AP.0 @AC0
  Scenario: Scenario 1.1 - NONE Access Process (no RoleToAccessProfiles on case-type)
    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with no RoleToAccessProfiles] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch]
      And a wait time of [5] seconds [to allow for Logstash to index the case just created].

    When a request is prepared with appropriate values,
     And the request [search for case type with legacy IDAM AccessProfiles],
     And it is submitted to call the [Global Search] operation of [CCD Data Store],

    Then a positive response is received,
     And the response [contains case with Access Process value NONE],
     And the response has all other details as expected.

  @S-1006.AP.1 @AC1
  Scenario: Scenario 1.2 - NONE Access Process (pseudo role assignments on case-type)
    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: for case type with pseudo role assignments] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch_AC_1]
      And a wait time of [5] seconds [to allow for Logstash to index the case just created].

    When a request is prepared with appropriate values,
     And the request [search for case type with matching pseudo role assignments],
     And it is submitted to call the [Global Search] operation of [CCD Data Store],

    Then a positive response is received,
     And the response [contains case with Access Process value NONE],
     And the response has all other details as expected.

  @S-1006.AP.2 @AC2
  Scenario: Scenario 2.1 - CHALLENGED Access Process (region and location filter on STANDARD RoleToAccessProfile for case-type)
    Given a user with [a caseworker with RA1: BASIC role assignment and RA2: STANDARD role assignment with region attribute with value 123 and location attribute with value 1]
      And a successful call [to create a case as a citizen: for case type with AccessProfile region filter: region = 3] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch_AC_2__Region_3]
      And a successful call [to create a case as a citizen: for case type with AccessProfile location filter: location = 2] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch_AC_2__Location_2]
      And a successful call [to create a case as a citizen: for case type with AccessProfile region and location filter: region = 123, location = 1] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch_AC_2__Region_123__Location_1]
      And a wait time of [5] seconds [to allow for Logstash to index the case just created].

    When a request is prepared with appropriate values,
     And the request [search for case type with STANDARD RoleToAccessProfile and case has unmatched region for user],
     And the request [search for case type with STANDARD RoleToAccessProfile and case has unmatched location for user],
     And the request [search for case type with STANDARD RoleToAccessProfile and case has matching region and location for user],
     And it is submitted to call the [Global Search] operation of [CCD Data Store],

    Then a positive response is received,
     And the response [contains two cases with Access Process value CHALLENGED when unmatched region/location],
     And the response [contains case with Access Process value NONE when matching region and location],
     And the response has all other details as expected.

  @S-1006.AP.3 @AC3
  Scenario: Scenario 3 - SPECIFIC Access Process (BASIC only RoleToAccessProfile for case-type)
    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: for case type with only BASIC AccessProfile] as in [F-1006_CreateCasePreRequisiteCitizen__FT_GlobalSearch_AC_3]
      And a wait time of [5] seconds [to allow for Logstash to index the case just created].

    When a request is prepared with appropriate values,
     And the request [search for case type with matching pseudo role assignments],
     And it is submitted to call the [Global Search] operation of [CCD Data Store],

    Then a positive response is received,
     And the response [contains case with Access Process value SPECIFIC],
     And the response has all other details as expected.
