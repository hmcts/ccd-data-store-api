@F-109
Feature: Add support in CCD role based authorisation for caseworker-caa

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-942
  Scenario: Must return /searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction1]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction1]
    And the response has all the details as expected

  @S-943
#    todo Write S-943 data file similar to s-942 that will query ES for the case created in F-109-Befta_Jurisdiction2_Case_Type1_Creation
  Scenario: Must return /searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction2]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction2]
    And the response has all the details as expected

  @S-944
#    todo
#    1 - create data file (F-109-Befta_Jurisdiction2_Case_Type2_Creation) to create a case for the case type BEFTA_CASETYPE_2_2 in jurisdiction Befta_Jurisdiction2
#    2 - write data file s-944 to query ES for this previously created case
  Scenario: Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (/searchCases)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type2_Creation]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role is not configured with the required CRUD permissions for Befta_Jurisdiction2_Case_Type2]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case Befta_Jurisdiction2_Case_Type2]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains no results]
    And the response has all the details as expected

#  Scenario 3 - Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (/searchCases)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which has not been configured with the required CRUD permissions for the case types of C1 or C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of either C1 or C2]
#
#  And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [only includes data relating to the case types for which Jamal has the required CRUD permissions for - ie no results are returned in this example]
#
#  And the response has all the details as expected
#
#
#
#
#
#  Scenario 4 - Must return /internal/searchCases values from Datastore for all jurisdictions for the given case type (1/2)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of C1 & C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of C1]
#
#  And it is submitted to call the [/internal/searchCases] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [includes data for the given case type for C1]
#
#  And the response has all the details as expected
#
#
#
#
#
#  Scenario 5 - Must return /internal/searchCases values from Datastore for all jurisdictions for the given case type (2/2)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of C1 & C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of C2]
#
#  And it is submitted to call the [/internal/searchCases] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [includes data for the given case type for C2]
#
#  And the response has all the details as expected
#
#
#  Scenario 6 - Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (/internal/searchCases)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which has not been configured with the required CRUD permissions for the case types of C1 or C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of either C1 or C2]
#
#  And it is submitted to call the [/internal/searchCases] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [only includes data relating to the case types for which Jamal has the required CRUD permissions for - ie no results are returned in this example]
#
#  And the response has all the details as expected
#
#
#
#
#
#  Scenario 7 - Must return /case-users values from Datastore for all jurisdictions for the given case type (1/2)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of C1 & C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of C1]
#
#  And it is submitted to call the [/case-users] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [includes data for the given case type for C1]
#
#  And the response has all the details as expected
#
#
#
#
#
#  Scenario 8 - Must return /case-users values from Datastore for all jurisdictions for the given case type (2/2)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of C1 & C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of C2]
#
#  And it is submitted to call the [/case-users] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [includes data for the given case type for C2]
#
#  And the response has all the details as expected
#
#
#  Scenario 9 - Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (/case-users)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which has not been configured with the required CRUD permissions for the case types of C1 or C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [contains the case type of either C1 or C2]
#
#  And it is submitted to call the [/case-users] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [only includes data relating to the case types for which Jamal has the required CRUD permissions for - ie no results are returned in this example]
#
#  And the response has all the details as expected
#
#
#
#
#
#
#
#  Scenario 10 - Must return /cases/{caseId}/supplementary-data values from Datastore for all jurisdictions and all case types
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of C1 & C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [does not contain any specific case type]
#
#  And it is submitted to call the [/cases/
#
#  {caseId}/supplementary-data] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [includes data for the case type of both C1 & C2]
#
#  And the response has all the details as expected
#
#
#
#
#
#  Scenario 11 - Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for a given case type (/cases/{caseId}/supplementary-data)
#  Given an appropriate test context as detailed in the test data source
#
#  And a user [Richard - with access to create cases for various jurisdictions eg Divorce & Probate]
#
#  And a successful call [by Richard to create a case (C1) for a jurisdiction eg Divorce] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a successful call [by Richard to create another case (C2) for a different jurisdiction eg Probate] as in [Prerequisite Case Creation Call for Case Assignment]
#
#  And a user [Jamal - a client with only the 'caseworker-caa' role which has not been configured with the required CRUD permissions for the case types of C1 and/or C2 on the definition file]
#
#  When a request is prepared with appropriate values
#
#  And the request [is made by Jamal via the new Manage Case Assignment (MCA) microservice]
#
#  And the request [does not contain any specific case type]
#
#  And it is submitted to call the [/cases/{caseId}/supplementary-data] operation of [CCD Data Store api]
#
#  Then a positive response is received
#
#  And the response [only includes data relating to the case types for which Jamal has the required CRUD permissions for - ie no results are returned in this example]
#
#  And the response has all the details as expected
  @S-new.1 @Ignore
  Scenario: must validate date in a right format
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-109_GetToken]
    When a request is prepared with appropriate values
    And the request [contains valid value for a formatted Date field]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has 201 return code]
    And the response has all other details as expected

  @S-new.2 @Ignore
  Scenario: must return an error for date value with invalid format
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [F-109_GetToken]
    When a request is prepared with appropriate values
    And the request [contains Date field with incorrect format]
    And it is submitted to call the [create case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has 422 return code]
    And the response has all other details as expected
