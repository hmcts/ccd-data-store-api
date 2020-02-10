@F-026
Feature: F-026: Get case data with UI layout

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source.

  @S-078
  Scenario: must return the list of cases and status code 200 for correct inputs
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains details of existing cases associated, along with an HTTP-200 OK],
    And the response has all other details as expected.

  @S-077
  Scenario: must return an empty SearchResultView envelope and status code 200 if case type has no associated cases
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [uses an existing case-type which doesn't have any associated cases],
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains an empty SearchResultView, along with an HTTP-200 OK],
    And the response has all other details as expected.

  @S-074
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given  a user with     [an active profile in CCD]
    When   a request is prepared with appropriate values
    And    the request     [does not provide valid authentication credentials]
    And    it is submitted to call the    [Get case data with UI layout]    operation of    [CCD Data Store]
    Then   a negative response is received
    And    the response    [has an HTTP-403 return code]
    And    the response has all other details as expected

  @S-075
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given  a user with     [an active profile in CCD],
    When   a request is prepared with appropriate values,
    And    the request     [does not provide authorised access to the operation],
    And    it is submitted to call the    [Get case data with UI layout]    operation of    [CCD Data Store],
    Then   a negative response is received,
    And    the response    [has an HTTP-403 return code],
    And    the response has all other details as expected.

  @S-076 @Ignore #This scenario is not returning http 412 code. Jira: RDM-6879
  Scenario: must return 412 when the case type is not present in Definition store workbasket input fields
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses case type which is not present in Definition store workbasket]
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has an HTTP-412 return code]
    And the response has all other details as expected
