@F-108 @elasticsearch
Feature: F-108: Elasticsearch external endpoint

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-900
  Scenario: should return the case for a role with same security classification as case type classification and read access on case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with security classification of PRIVATE],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains the previously created case data],
    And the response [does not contain fields with RESTRICTED security classification],
    And the response has all other details as expected.


  @S-901
  Scenario: should NOT return the case for a role with read access on case type and lower security classification than then case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with security classification of PUBLIC],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains no cases],
    And the response has all other details as expected.


  @S-903
  Scenario: should return the case for a role with read access to the case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with read access to the case state],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains the previously created case data],
    And the response has all other details as expected.


  @S-904
  Scenario: should NOT return the case for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_AAT_PRIVATE_B_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with no read access to the case state],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains no cases],
    And the response has all other details as expected.


  @S-905
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with security classification of RESTRICTED],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains the RESTRICTED email field value],
    And the response has all other details as expected.

  ### CrossCaseTypeSearch
  @S-910 @Ignore #wait for RDM-10885 ro run this
  Scenario: should return cases only for case types the user has access to - the user role can read case type and has same security classification as case type
    Given a case that has just been created as in [S-910_Create_Case_Private_Autotest1],
    And a case that has just been created as in [S-910_Create_Case_Private_Autotest2],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [private access to AUTOTEST1 jurisdiction only],
    When the request [is configured to search for both the previously created cases],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains only S-910_Create_Case_Private_Autotest1],
    And the response has all other details as expected.

  @S-911
  Scenario: should NOT return any cases for a role with read access on case types but lower security classification than the case types
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [public security classification access],
    When the request [is configured to search for both the previously created cases],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains no cases],
    And the response has all other details as expected.

  @S-912 @Ignore #wait for RDM-10885 ro run this
  Scenario: should return the cases for cross case type search for a role with read access to the case states
    Given a case that has just been created as in [S-912_Create_Case_Private_Autotest1],
    And a case that has just been created as in [S-912_Create_Case_Private_Autotest2],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [private multi jurisdiction access],
    When the request [is configured to search for both the previously created cases],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains details of 2 previously created cases],
    And the response [does not return the case field where user role has lower security classification than case field],
    And the response has all other details as expected.

  @S-913 @Ignore #wait for RDM-10885 ro run this
  Scenario: should NOT return any cases for cross case type search for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [no read access to the case state],
    When the request [is configured to search for both the previously created cases],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains no cases],
    And the response has all other details as expected.

  @S-914 @Ignore #wait for RDM-10885 ro run this
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [S-914_Create_Case_Private_Autotest1],
    And a case that has just been created as in [S-914_Create_Case_Private_Autotest2],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [restricted security classification],
    When the request [is configured to search for both the previously created cases],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains details of the restricted email field for the 2 previously created cases],
    And the response has all other details as expected.

  @S-915
  Scenario: cross case type search should return metadata only when source filter is not requested
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [multi jurisdiction access],
    When the request [is configured without a source filter],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains meta data of 2 previously created cases],
    And the response [does not return any case data],
    And the response has all other details as expected.


  ### Field Search Tests
  @S-916 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match in a date timefield
    Given a case that has just been created as in [S-916_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact date time from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previoulsy created case],
    And the response has all other details as expected.


  @S-917 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a date field
    Given a case that has just been created as in [S-917_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact date from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previoulsy created case],
    And the response has all other details as expected.

  @S-918 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Email field
    Given a case that has just been created as in [S-918_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact email from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previoulsy created case],
    And the response has all other details as expected.

  @S-919 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Fixed List field
    Given a case that has just been created as in [S-919_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact fixed list value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previoulsy created case],
    And the response has all other details as expected.

  @S-920 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Money field
    Given a case that has just been created as in [S-920_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact money field value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-921 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Number field
    Given a case that has just been created as in [S-921_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact number field value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-922 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a PhoneUK field
    Given a case that has just been created as in [S-922_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact PhoneUK value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-923 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Text Area field
    Given a case that has just been created as in [S-923_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact Text Area field value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-941 @Ignore #wait for RDM-10885 ro run this
  Scenario: Should return case for exact match on a Text field
    Given a case that has just been created as in [S-941_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact YesNo field value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-924
  Scenario: Should return case for exact match on a Text field
    Given a case that has just been created as in [S-924_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a valid profile],
    And the request [is configured to search for exact Text field value from previously created case],
    And a request is prepared with appropriate values,
    When it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-925
  Scenario: should return the case for a solicitor role if granted access to the case
    Given a case that has just been created as in [S-925_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a solicitor role],
    And a successful call [granting the user case access] as in [S-925_Grant_Case_Access],
    When the request [is configured to search for the previously created case],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains the previously created case],
    And the response has all other details as expected.

  @S-926
  Scenario: should NOT return the case for a solicitor role if not granted access to the case
    Given a case that has just been created as in [S-926_Create_Case_Private_Autotest1],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a solicitor role],
    And a user with [no case access granted to the case],
    When the request [is configured to search for the previously created case],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains no results],
    And the response has all other details as expected.

  @S-927
  Scenario: should NOT return the case for a role with same security classification as case type and no read access on case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_AAT_PRIVATE_B_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a senior role],
    And a user with [no read access to the case type],
    When the request [is configured to search for the previously created case],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then the response [contains no results],
    And the response has all other details as expected.

#-- Group Access GA-14 Tests -------------------------------------------------------------------------------------------------
  @S-948
  Scenario: User should be able to search a case as role assignment attribute and case caseAccessGroupId both match along with matching AccessProfiles
    Given a case that has just been created as in [GroupAccess_Full_Case_Creation_Data],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And a user with [a role with security classification of PRIVATE],
    When the request [is configured to search for the previously created case via exact match],
    And a request is prepared with appropriate values,
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API],
    Then a positive response is received,
    And the response [contains the previously created case data],
    And the response [does not contain fields with RESTRICTED security classification].

  @S-949 @Ignore
  Scenario: User should be able to search case as role assignment attribute and at least one of caseAccessGroupId in the case both match along with matching AccessProfiles
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [matches to an existing AccessProfiles 'caseworker-befta_master' in tab RoleToAccessProfiles in the CCD case definition file]
    And in CCD Case Definition [AuthorisationCaseType has a UserRole 'caseworker-befta_master' for a CaseType 'FT_MasterCaseType']
    And a case exists [with at least one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should be able to search the case as role assignment attribute match at least of caseAccessGroupId present in collection of CaseAccessGroup]

  @S-950 @Ignore
  Scenario: User should not be able to access case as caseAccessGroupId in not set for the case but role assignment has attribute set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And a case exists [which does not have any caseAccessGroupId value set]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute has caseAccessGroupId set but case does not have any caseAccessGroupId present in collection of CaseAccessGroup]

  @S-951 @Ignore
  Scenario: User should not be able to access case as collection of CaseAccessGroup is empty for the case but role assignment has attribute set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And a case exists[which has empty collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute has caseAccessGroupId set but case has empty collection of CaseAccessGroup]

  @S-951 @Ignore
  Scenario: User should not be able to access case if role assignment attribute and case caseAccessGroupId both don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute does match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-952 @Ignore
  Scenario: User should not be able to access case if role assignment attribute and none of caseAccessGroupId in the case don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with at least one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute don't match any of caseAccessGroupId present in collection of CaseAccessGroup]

  @S-953 @Ignore
  Scenario: User should not be able to access case if role assignment attribute is not set but case has caseAccessGroupId value
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment does not have any attribute set]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Access a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute is not set but caseAccessGroupId is set for the case]

  @S-954 @Ignore
  Scenario: User should be able to access case as caseAccessGroupId in not set for the case and role assignment is also not set
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment does not have any attribute set]
    And a case exists [which does not have any caseAccessGroupId value set]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should be able to access the case as role assignment attribute match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-955 @Ignore
  Scenario: User should not be able to access case as role assignment attribute and case caseAccessGroupId both don't match
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12346]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup]
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case as role assignment attribute don't match caseAccessGroupId set in collection of CaseAccessGroup]

  @S-956 @Ignore
  Scenario: User should not be able to access case as RoleName is missing in RoleToAccessProfiles
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [does not matches to an existing AccessProfiles in tab RoleToAccessProfiles in the CCD case definition file]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup which belongs to CaseType 'FT_MasterCaseType']
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case RoleName is missing in the RoleToAccessProfiles]

  @S-957 @Ignore
  Scenario: User should not be able to access case as UserRole is missing in AuthorisationCaseType
    Given a user with [an active profile in CCD]
    And the user has [Role Assignment has attribute having caseAccessGroupId set CIVIL:all-cases:123:12345]
    And the Role Assignment has [RoleName set to 'Role1'] which [matches to an existing AccessProfiles 'caseworker-befta_master' in tab RoleToAccessProfiles in the CCD case definition file]
    And in CCD Case Definition [AuthorisationCaseType does not have  matching UserRole]
    And a case exists [with only one caseAccessGroupId is set to CIVIL:all-cases:123:12345 in a collection of CaseAccessGroup which belongs to CaseType 'FT_MasterCaseType']
    When a request is prepared with appropriate values
    And a request is submitted to [Search a case] operation of [CCD Data Store],
    Then [User should not be able to access the case UserRole is missing in the AuthorisationCaseType]

#-- END Group Access Tests ---------------------------------------------------------------------------------------------
