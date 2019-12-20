@F-054
Feature: F-054: Get case for Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-093 # must return 200 and ist of case data for the given case id
  Scenario: must return 200 and  of case data for the given case id
    Given a user with [an active profile in CCD]
    And   a case that has just been created as in [Citizen_Full_Case_Creation_Data]
    When  a request is prepared with appropriate values
    And   the request [contains a case Id that has just been created above]
    And   it is submitted to call the [get case for citizen] operation of [CCD Data Store]
    Then  a positive response is received
    And   the response [code is HTTP-200]
    And   the response has all the details as expected


  @S-094
  Scenario:  must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [does not provide valid authentication credentials]
    And   it is submitted to call the [get case for citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-401]
    And   the response has all other details as expected

  @S-095
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   the request [provides authentic credentials without authorised access to the operation]
    And   it is submitted to call the [get case for citizen] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [code is HTTP-403]
    And   the response has all other details as expected
