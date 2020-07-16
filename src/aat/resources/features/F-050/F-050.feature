@F-050
Feature: F-050: Validate a set of fields as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-301
  Scenario: must validate when all fields are valid
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected

  @S-298
  Scenario: must not validate when field validation fails
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses a value that exceeds the field's max limit]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 422 return code]
    And the response has all other details as expected

  @S-300 @Ignore # re-write as part of RDM-6847
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses a dummy authorization token]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-299 @Ignore # re-write as part of RDM-6847
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses an invalid user Id that doesn’t exist in CCD]
    And the request [uses a dummy authorization token]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @Ignore
  #To be completed on https://tools.hmcts.net/jira/browse/RDM-6654
  @S-297
  Scenario: must not validate when CMC ExternalID is not unique or already exists
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [uses an existing CMC External Id]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 409 return code]
    And the response has all other details as expected

  @S-050.1
  Scenario: must validate date in a right format
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains valid value for a formatted Date field]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected

  @S-050.2
  Scenario: must return an error for date value with invalid format
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains Date field with incorrect format]
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 422 return code]
    And the response has all other details as expected
