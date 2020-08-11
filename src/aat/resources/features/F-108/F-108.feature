@F-108 @elasticsearch
Feature: Elasticsearch external endpoint

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-900
  Scenario: should return the case for a role with same security classification as case type classification and read access on case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with security classification of PRIVATE]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the previously created case data]
    And the response [does not contain fields with RESTRICTED security classification]
    And the response has all other details as expected


    @S-901
  Scenario: should NOT return the case for a role with read access on case type and lower security classification than then case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with security classification of PUBLIC]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected


    @S-903
  Scenario: should return the case for a role with read access to the case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with read access to the case state]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the previously created case data]
    And the response has all other details as expected


    @S-904
  Scenario: should NOT return the case for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with no read access to the case state]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected


    @S-905
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a role with security classification of RESTRICTED]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the RESTRICTED email field value]
    And the response has all other details as expected

  ### CrossCaseTypeSearch
    @S-910
  Scenario: should return cases only for case types the user has access to - the user role can read case type and has same security classification "
  + "as case type
    Given a case that has just been created as in [S-910_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-910_Create_Case_Private_Autotest2]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [private access to AUTOTEST1 jurisdiction only]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains only S-910_Create_Case_Private_Autotest1]
    And the response has all other details as expected

    @S-911
  Scenario: should NOT return any cases for a role with read access on case types but lower security classification than the case types
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [public security classification access]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected

    @S-912
  Scenario: should return the cases for cross case type search for a role with read access to the case states
    Given a case that has just been created as in [S-912_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-912_Create_Case_Private_Autotest2]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [private multi jurisdiction access]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains details of 2 previously created cases]
    And the response [does not return the case field where user role has lower security classification than case field]
    And the response has all other details as expected

    @S-913
  Scenario: should NOT return any cases for cross case type search for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [no read access to the case state]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected

    @S-914
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [S-914_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-914_Create_Case_Private_Autotest2]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [restricted security classification]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains details of the restricted email field for the 2 previously created cases]
    And the response has all other details as expected

    @S-915
  Scenario: cross case type search should return metadata only when source filter is not requested
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [multi jurisdiction access]
    When the request [is configured without a source filter]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains meta data of 2 previously created cases]
    And the response [does not return any case data]
    And the response has all other details as expected


  ### Field Search Tests

    @S-916
  Scenario: Should return case for exact match in a date timefield
    Given a case that has just been created as in [S-916_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact date time from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected


    @S-917
  Scenario: Should return case for exact match on a date field
    Given a case that has just been created as in [S-917_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact date from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-918
  Scenario: Should return case for exact match on a Email field
    Given a case that has just been created as in [S-918_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact email from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-919
  Scenario: Should return case for exact match on a Fixed List field
    Given a case that has just been created as in [S-919_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact fixed list value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-920
  Scenario: Should return case for exact match on a Money field
    Given a case that has just been created as in [S-920_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact money field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

    @S-921
  Scenario: Should return case for exact match on a Number field
      Given a case that has just been created as in [S-921_Create_Case_Private_Autotest1]
      And a wait time of 5 seconds [to allow for Logstash to index the case just created]
      And a user with [a valid profile]
      And the request [is configured to search for exact number field value from previously created case]
      And a request is prepared with appropriate values
      When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
      Then the response [contains the previously created case]
      And the response has all other details as expected

    @S-922
  Scenario: Should return case for exact match on a PhoneUK field
    Given a case that has just been created as in [S-922_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact PhoneUK value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

    @S-923
  Scenario: Should return case for exact match on a Text Area field
    Given a case that has just been created as in [S-923_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact Text Area field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

  @S-941
  Scenario: Should return case for exact match on a Text field
    Given a case that has just been created as in [S-941_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact YesNo field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

  @S-924
  Scenario: Should return case for exact match on a Text field
    Given a case that has just been created as in [S-924_Create_Case_Private_Autotest1]
    And a wait time of 5 seconds [to allow for Logstash to index the case just created]
    And a user with [a valid profile]
    And the request [is configured to search for exact Text field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected




