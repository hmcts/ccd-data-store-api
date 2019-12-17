@F-041
Feature: F-041: Get a list of printable documents for the given case id

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-055
  Scenario: must retrieve printable documents successfully for correct inputs
    Given a case that has just been created as in [F-041_Case_Creation_Data_With_Document]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [get a list of printable documents for the given case type] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a link to the printable documents that were uploaded to the case just created, along with a HTTP 200 OK]
    And the response has all other details as expected

  @S-057
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid authorization token]
    And it is submitted to call the [get a list of printable documents for the given case type] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains an HTTP-403 Forbidden]
    And the response has all other details as expected

  @S-056  @Ignore # defect RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials to the operation]
    And it is submitted to call the [get a list of printable documents for the given case type] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has an HTTP-401 Unauthorized]
    And the response has all other details as expected
