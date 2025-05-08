#===========================================================================
@F-065.AG #AccessMetadata
  Feature: F-065: Retrieve a Case by ID for Dynamic Display - Access Granted
#===========================================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-0
Scenario: Scenario 0 - User with IDAM role authorised to access case type, no RoleToAccessProfiles on case-type will return Access Granted: `STANDARD`

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: for case type with no RoleToAccessProfiles] as in [CreateCase_FT_GlobalSearch_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `STANDARD`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-1
Scenario: Scenario 1 - User's assignments: Role1/BASIC, R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1 will return Access Granted: `BASIC`

    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1] as in [CreateCase_FT_CaseAccess_1Role_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-2
Scenario: Scenario 2 - User's assignments: Role1/BASIC + Role2/SPECIFIC (granted), R2AP mappings: Role1 + Role2 -> AP1 + AP2, Authorised Case/State: AP1 + AP2 will return Access Granted: `BASIC,SPECIFIC`

    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: R2AP mappings: Role1 + Role2 -> AP1 + AP2, Authorised Case/State: AP1 + AP2] as in [CreateCase_FT_CaseAccess_2Roles_PreRequisiteCitizen]
      And a successful call [to grant SPECIFIC access to the caseworker] as in [GrantAccess_FT_CaseAccess_2Roles_Caseworker_SPECIFIC]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC,SPECIFIC`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-3
Scenario: Scenario 3 - User's assignments: Role1/STANDARD, R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1 will return Access Granted: `STANDARD`

    Given a user with [a caseworker with RA1: STANDARD role assignment]
      And a successful call [to create a case as a citizen: R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1] as in [CreateCase_FT_CaseAccess_1Role_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `STANDARD`],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-4
Scenario: Scenario 4 - User's assignments: Role1/BASIC + Role2/SPECIFIC (granted for a different case), R2AP mappings: Role1 + Role2 -> AP1 + AP2, Authorised Case/State: AP1 + AP2 will return Access Granted: `BASIC`

    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: R2AP mappings: Role1 + Role2 -> AP1 + AP2, Authorised Case/State: AP1 + AP2] as in [CreateCase_FT_CaseAccess_2Roles_PreRequisiteCitizen]
      And a successful call [to create a 2nd case as a citizen: R2AP mappings: Role1 + Role2 -> AP1 + AP2, Authorised Case/State: AP1 + AP2] as in [CreateCase_FT_CaseAccess_2Roles_PreRequisiteCitizen__2ndCase]
      And a successful call [to grant SPECIFIC access to the caseworker to the 2nd case] as in [GrantAccess_FT_CaseAccess_2Roles_Caseworker_SPECIFIC__2ndCase]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the first case created above],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC` [Due to Role2 not passing filtering]],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-5
Scenario: Scenario 5 - User's assignments: Role1/BASIC + Role2/SPECIFIC (granted), R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1 will return Access Granted: `BASIC`

    Given a user with [a caseworker with RA1: BASIC role assignment]
      And a successful call [to create a case as a citizen: R2AP mappings: Role1 -> AP1, Authorised Case/State: AP1] as in [CreateCase_FT_CaseAccess_1Role_PreRequisiteCitizen]
      And a successful call [to grant SPECIFIC access to the caseworker] as in [GrantAccess_FT_CaseAccess_1Role_Caseworker_SPECIFIC]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC` [Due to no mapping for Role2]],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-6.1
Scenario: Scenario 6.1 - User's assignments: Role1/BASIC + Role2/SPECIFIC (granted) + Role3/CHALLENGED (Region 123), R2AP mappings: Role1 + Role2 + Role 3 -> AP1 + AP2 + AP3, Authorised Case/State: AP1 + AP2 (state: Created only) + AP3 will return Access Granted: `BASIC` (as wrong region)

    Given a user with [a caseworker with RA1: BASIC + RA3: CHALLENGED (Region 123) role assignment]
      And a successful call [to create a case as a citizen in Region 3: R2AP mappings: Role1 + Role2 + Role3 -> AP1 + AP2 + AP3, Authorised Case/State: AP1 + AP3] as in [CreateCase_FT_CaseAccess_3Roles_PreRequisiteCitizen__Region_3]
      And a successful call [to grant SPECIFIC access to the caseworker] as in [GrantAccess_FT_CaseAccess_3Roles_Caseworker_SPECIFIC__Region_3]
      And a successful call [to get case before case is updated to check `Access Granted` set to `BASIC,SPECIFIC` [Due to CHALLENGED in wrong region]] as in [S-065.AG-6.1_before_case_update]
      And a successful call [to update the case] as in [UpdateCase_FT_CaseAccess_3Roles__Region_3]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC` [Due to AP2 not being authorised for case state, and CHALLENGED in wrong region]],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-6.2
Scenario: Scenario 6.2 - User's assignments: Role1/BASIC + Role2/SPECIFIC (granted) + Role3/CHALLENGED (Region 123), R2AP mappings: Role1 + Role2 + Role 3 -> AP1 + AP2 + AP3, Authorised Case/State: AP1 + AP2 (state: Created only) + AP3 will return Access Granted: `BASIC,CHALLENGED`

    Given a user with [a caseworker with RA1: BASIC + RA3: CHALLENGED (Region 123) role assignment]
      And a successful call [to create a case as a citizen in Region 123: R2AP mappings: Role1 + Role2 + Role3 -> AP1 + AP2 + AP3, Authorised Case/State: AP1 + AP3] as in [CreateCase_FT_CaseAccess_3Roles_PreRequisiteCitizen__Region_123]
      And a successful call [to grant SPECIFIC access to the caseworker] as in [GrantAccess_FT_CaseAccess_3Roles_Caseworker_SPECIFIC__Region_123]
      And a successful call [to get case before case is updated to check `Access Granted` set to `BASIC,CHALLENGED,SPECIFIC`] as in [S-065.AG-6.2_before_case_update]
      And a successful call [to update the case] as in [UpdateCase_FT_CaseAccess_3Roles__Region_123]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `BASIC,CHALLENGED` [Due to AP2 not being authorised for case state]],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-065.AG-7
Scenario: Scenario 7 - User with IDAM role authorised to access case type, R2AP mappings: IdamRole -> AP1, Authorised Case/State: AP1 will return Access Granted: `STANDARD`

    Given a user with [a caseworker with no role assignments]
      And a successful call [to create a case as a citizen: R2AP mappings: IdamRole -> AP1, Authorised Case/State: AP1] as in [CreateCase_FT_CaseAccess_IdamRole_PreRequisiteCitizen]

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains `Access Granted` set to `STANDARD`],
      And the response has all other details as expected.
