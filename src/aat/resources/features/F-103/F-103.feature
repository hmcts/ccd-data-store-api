@F-103
<<<<<<< HEAD
Feature: Elasticsearch external endpoint

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-600 @elasticsearch
  Scenario: should return the case for a role with same security classification as case type classification and read access on case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [a role with security classification of PRIVATE]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the previously created case data]
    And the response [does not contain fields with RESTRICTED security classification]
    And the response has all other details as expected


    @S-601
  Scenario: should NOT return the case for a role with read access on case type and lower security classification than then case type
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [a role with security classification of PUBLIC]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected


    @S-603
  Scenario: should return the case for a role with read access to the case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [a role with read access to the case state]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the previously created case data]
    And the response has all other details as expected


    @S-604
  Scenario: should NOT return the case for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [a role with no read access to the case state]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected


    @S-605
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [a role with security classification of RESTRICTED]
    When the request [is configured to search for the previously created case via exact match]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains the RESTRICTED email field value]
    And the response has all other details as expected

  ### CrossCaseTypeSearch
    @S-610
  Scenario: should return cases only for case types the user has access to - the user role can read case type and has same security classification "
  + "as case type
    Given a case that has just been created as in [S-610_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-610_Create_Case_Private_Autotest2]
    And logstash has finished indexing case data
    And a user with [private access to AUTOTEST1 jurisdiction only]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains only 1 case]
    And the response has all other details as expected

    @S-611
  Scenario: should NOT return any cases for a role with read access on case types but lower security classification than the case types
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data]
    And logstash has finished indexing case data
    And a user with [public security classification access]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected

    @S-612
  Scenario: should return the cases for cross case type search for a role with read access to the case states
    Given a case that has just been created as in [S-612_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-612_Create_Case_Private_Autotest2]
    And logstash has finished indexing case data
    And a user with [private multi jurisdiction access]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains details of 2 previously created cases]
    And the response [does not return the case field where user role has lower security classification than case field]
    And the response has all other details as expected

    @S-613
  Scenario: should NOT return any cases for cross case type search for a role with no read access to a case state
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And a case that has just been created as in [Private_Case_Creation_Autotest2_Data]
    And logstash has finished indexing case data
    And a user with [no read access to the case state]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all other details as expected

    @S-614
  Scenario: should return the case field where user role matches ACL and security classification
    Given a case that has just been created as in [S-614_Create_Case_Private_Autotest1]
    And a case that has just been created as in [S-614_Create_Case_Private_Autotest2]
    And logstash has finished indexing case data
    And a user with [restricted security classification]
    When the request [is configured to search for both the previously created cases]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains details of the restricted email field for the 2 previously created cases]
    And the response has all other details as expected

    @S-615
  Scenario: cross case type search should return metadata only when source filter is not requested
    Given a case that has just been created as in [Private_Case_Creation_Autotest1_Data]
    And logstash has finished indexing case data
    And a user with [multi jurisdiction access]
    When the request [is configured without a source filter]
    And a request is prepared with appropriate values
    And it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then a positive response is received
    And the response [contains meta data of 2 previously created cases]
    And the response [does not return any case data]
    And the response has all other details as expected


  ### Field Search Tests

    @S-616
  Scenario: Should return case for exact match in a date timefield
    Given a case that has just been created as in [S-616_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact date time from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected


    @S-617
  Scenario: Should return case for exact match on a date field
    Given a case that has just been created as in [S-617_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact date from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-618
  Scenario: Should return case for exact match on a Email field
    Given a case that has just been created as in [S-618_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact email from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-619
  Scenario: Should return case for exact match on a Fixed List field
    Given a case that has just been created as in [S-619_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact fixed list value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previoulsy created case]
    And the response has all other details as expected

    @S-620
  Scenario: Should return case for exact match on a Money field
    Given a case that has just been created as in [S-620_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact money field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

    @S-621
  Scenario: Should return case for exact match on a Number field
      Given a case that has just been created as in [S-621_Create_Case_Private_Autotest1]
      And logstash has finished indexing case data
      And a user with [a valid profile]
      And the request [is configured to search for exact number field value from previously created case]
      And a request is prepared with appropriate values
      When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
      Then the response [contains the previously created case]
      And the response has all other details as expected

    @S-622
  Scenario: Should return case for exact match on a PhoneUK field
    Given a case that has just been created as in [S-622_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact PhoneUK value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected

    @S-623
  Scenario: Should return case for exact match on a Text Area field
    Given a case that has just been created as in [S-623_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact Text Area field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected


  @S-624
  Scenario: Should return case for exact match on a Text field
    Given a case that has just been created as in [S-624_Create_Case_Private_Autotest1]
    And logstash has finished indexing case data
    And a user with [a valid profile]
    And the request [is configured to search for exact Text field value from previously created case]
    And a request is prepared with appropriate values
    When it is submitted to call the [internal search query] operation of [CCD Data Store Elastic Search API]
    Then the response [contains the previously created case]
    And the response has all other details as expected
=======
Feature: F-103: Get Case-Assigned Users and Roles

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-597
  Scenario: when a specific User ID is supplied for a specific case ID, then the case roles relating only to the User ID for that specific Case ID must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & Dil's User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil, as per above],
    And the response has all other details as expected.

  @S-598
  Scenario: when the invoking user is not a privileged user but the request includes his/her own User ID, then the invoker's case roles for the case should be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Dil with the Case ID of C1 & Dil's own User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil, as per above],
    And the response has all other details as expected.

  @S-599
  Scenario: when no User ID is supplied for a specific case ID, then the case roles relating to all  users with access to that case must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Steve - with an active profile],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    And a successful call [by Jamal to assign Steve a few case roles to access C1] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & no User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil & Steve, as per above],
    And the response has all other details as expected.

  @S-600
  Scenario: when no User ID is supplied for a list of Case IDs, then the case roles relating to all users with access to all listed cases must be returned
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a case [C2, which has just been] created as in [F103_Case_Data_Create_C2],
    And a case [C3, which has just been] created as in [F103_Case_Data_Create_C3],
    And a user [Dil - with an active profile],
    And a user [Steve - with an active profile],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a successful call [by Jamal to assign Dil a few case roles to access C1] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C1],
    And a successful call [by Jamal to assign Dil a few case roles to access C2] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C2],
    And a successful call [by Jamal to assign Dil a few case roles to access C3] as in [F-103_Jamal_Assign_Dil_Case_Role_To_C3],
    And a successful call [by Jamal to assign Steve a few case roles to access C1] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C1],
    And a successful call [by Jamal to assign Steve a few case roles to access C2] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C2],
    And a successful call [by Jamal to assign Steve a few case roles to access C3] as in [F-103_Jamal_Assign_Steve_Case_Role_To_C3],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with Case IDs of C1, C2 & C3 & no User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a positive response is received,
    And the response [contains the list of case roles just granted to Dil & Steve for C1, C2 & C3, as per above],
    And the response has all other details as expected.

  @S-601
  Scenario: must return an error response for a missing Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - with a valid User ID],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with no Case ID & Dil's User ID]
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.


  @S-602
  Scenario: must return an error response for a malformed Case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Dil - with a valid User ID],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with a malformed Case ID & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.

  @S-603
  Scenario: must return an error response for a malformed User ID List (e.g. user1,user2,,user4)
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a user [Jamal -  who is a privileged user with permissions to access the case assignments of other users],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    When a request is prepared with appropriate values,
    And the request [is made by Jamal with the Case ID of C1 & a malformed User ID list],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.

  @S-604
  Scenario: must return an error response when the invoker does not have the required IDAM role(s) to query the role assignments for users listed in the query
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - who can create a case],
    And a case [C1, which has just been] created as in [F103_Case_Data_Create_C1],
    And a user [Dil - with an active profile],
    And a user [Steve -  who is not a privileged user and does not have permissions to access the case assignments of other users],
    When a request is prepared with appropriate values,
    And the request [is made by Steve with the Case ID of C1 & Dil's User ID],
    And it is submitted to call the [Get Case-Assigned Users and Roles] operation of [CCD Data Store api],
    Then a negative response is received,
    And the response has all other details as expected.
>>>>>>> refs/heads/develop

