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

  @S-074 @Ignore # re-write as part of RDM-6847
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given  a user with     [an active profile in CCD]
    When   a request is prepared with appropriate values
    And    the request     [does not provide valid authentication credentials]
    And    it is submitted to call the    [Get case data with UI layout]    operation of    [CCD Data Store]
    Then   a negative response is received
    And    the response    [has an HTTP-403 return code]
    And    the response has all other details as expected

  @S-075 @Ignore # re-write as part of RDM-6847
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

  @S-587 @RDM-7793 # Get cases with Last State Modified Date field as a column
    # Also update this with data ordering when FW support is available
  Scenario: must return the list of cases and status code 200 for correct inputs
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains details of existing cases associated, along with an HTTP-200 OK],
    And the response [contains Last State Modified Date as a column for UI layout]
    And the response has all other details as expected.

  @S-588 @RDM-7739 #Get cases list from Last State Modified Date filter - when data list 1 or more
  # Enable this after changing it to use Dynamic date instead of a static one
  # Also update this with data ordering when FW support is available
  Scenario: must return the list of cases and status code 200 for correct inputs
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-026.02_GetToken]
    And a case that has just been created as in [S-026.02_Case]
    When a request is prepared with appropriate values,
    And the request [has Last State Modified Date filter]
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains details of existing cases associated, along with an HTTP-200 OK],
    And the response [contains atleast one result item]
    And the response has all other details as expected.

  @S-589  #Get cases list from Last State Modified Date filter - when data list empty
  Scenario: must return the list of cases and status code 200 for correct inputs
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values,
    And the request [has Last State Modified Date filter]
    And it is submitted to call the [Get case data with UI layout] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains details of existing cases associated, along with an HTTP-200 OK],
    And the response [contains empty results list]
    And the response has all other details as expected.
