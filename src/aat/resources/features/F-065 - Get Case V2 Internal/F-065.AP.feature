#===========================================================================
@F-065.AP
  Feature: F-065: Retrieve a Case by ID for Dynamic Display - Access Process
#===========================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AP.0
Scenario: Scenario 1.1 - NONE Access Process (no RoleToAccessProfiles on case-type)

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with no RoleToAccessProfiles] as in [CreateCase_FT_GlobalSearch_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [contains the case that has just been created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Process` set to `NONE`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AP.1
  Scenario: Scenario 1.2 - NONE Access Process (pseudo role assignments on case-type)

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with pseudo role assignments] as in [CreateCase_FT_GlobalSearch_AC_1_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [contains the case that has just been created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Process` set to `NONE`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AP.2.1
  Scenario: Scenario 2.1 - CHALLENGED Access Process (unmatched region filter on STANDARD RoleToAccessProfile for case-type)

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with AccessProfile region filter: region = 3] as in [CreateCase_FT_GlobalSearch_AC_2_PreRequisiteCitizen__Region_3]

     When a request is prepared with appropriate values,
      And the request [contains the case that has just been created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Process` set to `CHALLENGED`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AP.2.2
  Scenario: Scenario 2.2 - NONE Access Process (matched region filter on STANDARD RoleToAccessProfile for case-type)

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with AccessProfile region filter: region = 123] as in [CreateCase_FT_GlobalSearch_AC_2_PreRequisiteCitizen__Region_123]

     When a request is prepared with appropriate values,
      And the request [contains the case that has just been created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Process` set to `NONE`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AP.3
  Scenario: Scenario 3 - SPECIFIC Access Process (BASIC only RoleToAccessProfile for case-type)

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with only BASIC AccessProfile] as in [CreateCase_FT_GlobalSearch_AC_3_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [contains the case that has just been created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Process` set to `SPECIFIC`],
      And the response has all other details as expected.
