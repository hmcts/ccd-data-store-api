@F-057
Feature: F-057: Get the pagination metadata for a case data search for Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-140
  Scenario: must return pagination metadata successfully for correct inputs
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [provides correct inputs]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected

  @S-137
  Scenario: must return appropriate negative response when request does not provide a valid sort direction
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid sort direction]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all other details as expected

  @S-138 @Ignore # re-write as part of RDM-6847
  Scenario: must return appropriate negative response when request does not provide a valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid authentication credentials]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-139 @Ignore # re-write as part of RDM-6847
  Scenario: must return appropriate negative response when request does not provide an authorized access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide an authorized access to the operation]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-542
  Scenario: must return appropriate negative response when casefield does not start with “case.”
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid case-field which starts with “case.”]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all other details as expected

  @S-543
  Scenario: must return appropriate negative response when request provides an invalid security classification
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide a valid security classification]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all other details as expected

  @S-057.01 @Ignore @RDM-7739 # Pagination data with Last State Modified Date filter - with results
    # Enable this after changing it to use Dynamic date instead of a static one
  Scenario: must return pagination metadata successfully for correct Last State Modified State input

  @S-594 # Pagination data with Last State Modified Date filter - no results
  Scenario: must return pagination metadata successfully for correct inputs
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [provides correct inputs]
    And it is submitted to call the [Get the pagination metadata for a case data search for Citizen] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected
