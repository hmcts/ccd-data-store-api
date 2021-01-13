@F-109
Feature: F-109: Role-Based Authorisation of Caseworker CAAs

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-942 @elasticsearch @Ignore # Fix for LAST_STATE_MODIFIED_DATE coming in 19.1
  Scenario: Must return /searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction1]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction1]
    And the response has all the details as expected

  @S-943 @elasticsearch @Ignore # Fix for LAST_STATE_MODIFIED_DATE coming in 19.1
  Scenario: Must return /searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction2]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction2]
    And the response has all the details as expected

  @S-944 @elasticsearch @Smoke
  Scenario: Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (/searchCases)
    Given a user [with access to create case for Befta_Jurisdiction3]
    And a case that has just been created as in [F-109-Befta_Jurisdiction3_Case_Type1_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role is not configured with the required CRUD permissions for Befta_Jurisdiction3]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case Befta_Jurisdiction3_Case_Type1]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains no results]
    And the response has all the details as expected

  @S-945 @elasticsearch @Ignore # Fix for LAST_STATE_MODIFIED_DATE coming in 19.1
  Scenario: Must return internal/searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction1]
    And it is submitted to call the [internal/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction1]
    And the response has all the details as expected

  @S-946 @elasticsearch @Ignore # Fix for LAST_STATE_MODIFIED_DATE coming in 19.1
  Scenario: Must return internal/searchCases values from Datastore for all jurisdictions for the given case type (1/2)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction1_Case_Creation]
    And a case that has just been created as in [F-109-Befta_Jurisdiction2_Case_Type1_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role which is configured with the required CRUD permissions for the case types of both previously created cases]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case from Jurisdiction Befta_Jurisdiction2]
    And it is submitted to call the [internal/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains the case type of Jurisdiction Befta_Jurisdiction2]
    And the response has all the details as expected

  @S-947 @elasticsearch @Ignore
  Scenario: Must return a positive response when required CRUD permissions have not been configured for the caseworker-caa for the case type (internal/searchCases)
    Given a user [with access to create cases for various jurisdictions Befta_Jurisdiction1 & Befta_Jurisdiction2]
    And a case that has just been created as in [F-109-Befta_Jurisdiction3_Case_Type1_Creation_Token_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    And a user [with only the 'caseworker-caa' role is not configured with the required CRUD permissions for Befta_Jurisdiction3_Case_Type1]
    When a request is prepared with appropriate values
    And the request [is made to query the previously created case Befta_Jurisdiction3_Case_Type1]
    And it is submitted to call the [internal/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the request [contains no results]
    And the response has all the details as expected

  @S-105.1.2
  Scenario: /case-users endpoint can be used with caseworker-caa role
    Given an appropriate test context as detailed in the test data source,
    And a user [Admin - who has only caseworker-caa role],
    And a user [Richard - who can create a case],
    And a user [Olawale - with an active solicitor profile],
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And a successful call [by Richard to create a case - C1] as in [F-105_Prerequisite_Case_Creation_Call_for_Case_Assignment],
    When a request is prepared with appropriate values,
    And the request [is made from an authorised application, by Admin, with the Case ID of C1, User ID of Olawale and a proper Case Role CR-1],
    And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store Api],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify Olawale's reception of the role CR-1 over the case C1] will get the expected response as in [S-105.1_Get_Case_Roles_for_Case_C1].
