@F-041
Feature: F-041: Get a list of printable documents for the given case type

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-055
  Scenario: must retrieve printable documents successfully for correct inputs
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case]
    When a request is prepared with appropriate values
    And the request [uses the case-reference of the case just created]
    And it is submitted to call the [printable documents of the case just created] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the document details of the case just created]
    And the response has all other details as expected

  @S-057
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [printable documents of the case just created] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-056
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [printable documents of the case just created] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected
