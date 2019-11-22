@F-050
Feature: Validate a set of fields as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-301
  Scenario: must validate when all fields are valid
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected

  @Ignore
  #To be completed on https://tools.hmcts.net/jira/browse/RDM-6654
  @S-297
  Scenario: must not validate when CMC ExternalID is not unique / already exists
    Given a user with a detailed profile in CCD
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-298
  Scenario: must not validate when field validation fails
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-300
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected

  @S-299
  Scenario: must return 4xx when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response has all the details as expected
