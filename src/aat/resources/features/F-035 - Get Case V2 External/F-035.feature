#====================================
@F-035
Feature: F-035: Retrieve a case by id
#====================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-159
Scenario: should retrieve case when the case reference exists

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains the details of the case just created, along with an HTTP-200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-155 @Ignore # defect RDM-6628
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid user authorisation token],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-401 Unauthorised],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-156
Scenario: must return 404 when request provides authentic credentials without authorised access to the operation

    Given a case that has just been created as in [S-156_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a valid user authorisation token without access to the operation],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-157
Scenario: should get 400 when case reference invalid

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case reference],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-158
Scenario: should get 404 when case reference does not exist

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a case reference that does not exist],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-591
Scenario: must return status 200 along with the case-view object successfully

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [S-035.01_Case],

     When a request is prepared with appropriate values,
      And the request [uses the case-reference of the case just created],
      And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains Last State Modified Date metadata field],
      And the response has all other details as expected.

#-- Group Access GA-13 Tests -------------------------------------------------------------------------------------------------
  @S-960
  Scenario: User should be able to access a case as role assignment attribute and case caseAccessGroupId both match along with matching AccessProfiles
    Given a case that has just been created as in [GroupAccess_Full_Case_Creation_Data],
    And a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And the request [contains the case reference of the case just created],
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [contains the details of the case just created, along with an HTTP-200 OK]

  @S-961 @Ignore
  Scenario: User should be able to access case as role assignment attribute and at least one of caseAccessGroupId in the case both match along with matching AccessProfiles
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set to CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [matches to an existing AccessProfiles 'caseworker-befta_master' in tab RoleToAccessProfiles in the CCD case definition file]
    And in CCD Case Definition [AuthorisationCaseType has a UserRole 'caseworker-befta_master' for a CaseType 'FT_MasterCaseType']
    And a case exists [with at least one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should be able to access the case as role assignment attribute match at least of caseAccessGroupId present in collection of CaseAccessGroup]

  @S-962 @Ignore
  Scenario: User should not be able to access case as caseAccessGroupId in not set for the case but role assignment has attribute set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set to CIVIL:all-cases:123:12345]
    And a case exists [which does not have any caseAccessGroupId value set]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute has caseAccessGroupId set but case does not have any caseGroupId present in collection of CaseAccessGroup]

  @S-963 @Ignore
  Scenario: User should not be able to access case as collection of CaseAccessGroup is empty for the case but role assignment has attribute set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set to CIVIL:all-cases:123:12345]
    And a case exists[which has empty collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute has caseAccessGroupId set but case has empty collection of CaseAccessGroup]

  @S-964 @Ignore
  Scenario: User should not be able to access case if role assignment attribute and case caseAccessGroupId both don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute does match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-965 @Ignore
  Scenario: User should not be able to access case if role assignment attribute and none of caseAccessGroupId in the case don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with at least one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute don't match any of caseAccessGroupId present in collection of CaseAccessGroup]

  @S-966 @Ignore
  Scenario: User should not be able to access case if role assignment attribute is not set but case has caseAccessGroupId value
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment does not have any attribute set]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute is not set but caseAccessGroupId is set for the case]

  @S-967 @Ignore
  Scenario: User should be able to access case as caseAccessGroupId in not set for the case and role assignment is also not set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment does not have any attribute set]
    And a case exists [which does not have any caseAccessGroupId value set]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should be able to access the case as role assignment attribute match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-968 @Ignore
  Scenario: User should not be able to access case as role assignment attribute and case caseAccessGroupId both don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute don't match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-969 @Ignore
  Scenario: User should not be able to access case as RoleName is missing in RoleToAccessProfiles
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [does not matches to an existing AccessProfiles in tab RoleToAccessProfiles in the CCD case definition file]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup which belongs to CaseType 'FT_MasterCaseType']
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case RoleName is missing in the RoleToAccessProfiles]

  @S-970 @Ignore
  Scenario: User should not be able to access case as UserRole is missing in AuthorisationCaseType
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [matches to an existing AccessProfiles 'caseworker-befta_master' in tab RoleToAccessProfiles in the CCD case definition file]
    And in CCD Case Definition [AuthorisationCaseType does not have  matching UserRole]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup which belongs to CaseType 'FT_MasterCaseType']
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case UserRole is missing in the AuthorisationCaseType]

#-- END Group Access Tests ---------------------------------------------------------------------------------------------
