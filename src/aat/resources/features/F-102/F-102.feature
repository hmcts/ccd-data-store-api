@F-102
Feature: F-102: Get jurisdictions available to the user

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-533
  Scenario: must return a list of jurisdictions for a valid user
    Given a user with [an active profile in CCD having create case access for a jurisdiction]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access parameter]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains HTTP 200 Ok status code]
    And the response [contains the list of jurisdictions a user has access to]
    And the response has all other details as expected

  @S-534 @Ignore
  Scenario: No jurisdictions found for given access criteria
    #We will never get a "No jurisdictions found for given access criteria" in real time scenario.
    #Hence skipping the scenario implementation.

  @S-535 @Ignore # This endpoint is returning 403. Will be fixed as a part of RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [no profile in CCD]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains the HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-536 @Ignore # re-write as part of RDM-6847
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid authorization token]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains the HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-559
  Scenario: must return 400 if access type is not in create, read or update
    Given a user with [a detailed profile in CCD having create case access for a jurisdiction]
    When a request is prepared with appropriate values
    And the request [has DELETE as case access parameter]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains HTTP 400 Bad Request]
    And the response [contains an error message : Access can only be 'create', 'read' or 'update']
    And the response has all other details as expected

  @S-580
  Scenario: must return a list of jurisdictions for a valid user with no user profile
    Given a user with [appropriate idam roles but no CCD user profile]
    When a request is prepared with appropriate values
    And the request [has CREATE as case access parameter]
    And it is submitted to call the [Get jurisdictions available to the user] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains the list of jurisdictions a user has access to]
    And the response has all other details as expected
