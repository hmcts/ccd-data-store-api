@F-104 @elasticsearch
Feature: External Search API

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    #possitive request scenario of each type
  @S-625
  Scenario: Usecase request using SearchResultsFields useCase returns correct fields
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using the query parameter usecase=search]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the field headers as specified in the SearchResultsFields only]
    And the response [contains the field data as specified in the SearchResultsFields]
    And the response [contains the field data of all meta data fields]
    And the response has all other details as expected


  @S-626
  Scenario: Usecase request using WorkbasketResultsFields useCase returns correct fields
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using the query parameter usecase=workbasket]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the field headers as specified in the WorkbasketResultsFields only]
    And the response [contains the field data as specified in the WorkbasketResultsFields]
    And the response [contains the field data of all meta data fields]
    And the response has all other details as expected


  @S-627
  Scenario: Usecase request using SearchCasesResultFields useCase returns correct fields
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the fields as specified in the SearchCasesResultFields only]
    And the response [contains the field data as specified in the SearchCasesResultFields]
    And the response [contains the field data of all meta data fields]
    And the response has all other details as expected


  @S-628
  Scenario: Standard request return all fields in a case user has access to
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using not using any use case query parameter]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains all the field headers for the case]
    And the response [contains all the field data for the case]
    And the response has all other details as expected


    @S-629
  Scenario: Standard Request with specified fields return only headers and data for fields specified
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    When the request [is configured to return specific fields]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for the specified fields]
    And the response [contains data for only the specified fields]
    And the response [contains data for all meta data fields]
    And the response has all other details as expected


    @S-630
  Scenario: Usecase request with specified fields acts as a Standard Request
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    When the request [is configured to return specific fields]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for the specified fields]
    And the response [contains data for only the specified fields]
    And the response [contains data for and all meta data fields]
    And the response has all other details as expected


    @S-631 #senior no access to number field due to role in caseAuth
  Scenario: usecase request with no access to field via user role in CaseAuthTab
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a caseworker-autotest1-senior role]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using the query parameter usecase=search]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for fields the user has role access to]
    And the response [contains data only for fields the user has role access to]
    And the response has all other details as expected


    @S-632 #senior role only has access to money field through user rol on result tab (workbasket )
  Scenario: usecase request with no access to field via user role in Result Fields config tab
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a caseworker-autotest1-senior role]
    And a request is prepared with appropriate values
    When the request [is configured to search for the previously created case via exact match]
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    And the request [is using the query parameter usecase=workbasket]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for fields the user has role access to]
    And the response [contains data only for fields the user has role access to]
    And the response has all other details as expected


    @S-633 #private user cant see email field
  Scenario: usecase request with no access to field via Security Classification
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with security classification of PRIVATE]
    When the request [is configured to search for the previously created case via exact match]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for fields the user has SC access to]
    And the response [contains data only for fields the user has SC access to]
    And the response has all other details as expected


    @S-634
  Scenario: standard request specifying field that user doesn't have access to
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created case via exact match]
    When the request [is configured to return specific fields that the user does not have access to]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains headers only for the specified fields the user has access to]
    And the response [contains data for only the specified fields the user has access to]
    And the response [contains data for and all meta data fields]
    And the response has all other details as expected


    @S-635
  Scenario: Usecase request will return cases ordered as per relevant definition configuration
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering1_2]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering1_1]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering1_3]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created cases]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains cases ordered as per definition configuration]
    And the response has all other details as expected


    @S-636
  Scenario: Usecase request default ordering can be overridden in the request
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering2_2]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering2_1]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering2_3]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created cases]
    And the request [is using the query parameter usecase=orgcases]
    And the request [is configured to return cases opposite from the default]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains cases ordered as per the request configuration]
    And the response has all other details as expected


  @S-637 # oldest case first normally - we want to order by newest case created first
  Scenario: Standard request can be ordered by metadata field
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering3_2]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering3_1]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Ordering3_3]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for the previously created cases]
    And the request [is configured to order by a meta data field]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains cases in ordered as per request configuration]
    And the response has all other details as expected

  @S-638
  Scenario: all CaseType Headers are returned even if no cases are found for a standard search
    And a user with [a valid user profile]
    When the request [is configured to search for a case that doesn't exist]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains all the header fields for that case type]
    And the response [contains no case data]
    And the request [lists total cases as 0]
    And the response has all other details as expected

  @S-639
  Scenario: configured CaseType Headers are returned even if no cases are found for a usecase search
    And a user with [a valid user profile]
    When the request [is configured to search for a case that doesn't exist]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the header fields for that case type as per definition configuration]
    And the response [contains no case data]
    And the request [lists total cases as 0]
    And the response has all other details as expected

  @S-640
  Scenario: Request can be sent with paginated search criteria
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Pagination1]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Pagination2]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Pagination3]
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data_Pagination4]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid user profile]
    When the request [is configured to search for previously created cases using pagination criteria]
    And the request [is using the query parameter usecase=orgcases]
    And a request is prepared with appropriate values
    And it is submitted to call the [external search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the cases as per the request pagination criteria]
    And the response has all other details as expected

