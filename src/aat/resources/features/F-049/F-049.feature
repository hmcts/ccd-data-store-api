@F-049
Feature: Start case creation as Case worker

  @S-235
  Scenario: must start case creation process successfully for correct inputs
    Given   an appropriate test context as detailed in the test data source,
    And    a user with an existing case in CCD,
    When    a request is prepared with appropriate values,
    And    it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a positive response is received,
    And    the response has all the details as expected.

  @S-233
  Scenario: must return 404 when no case found for the given Idam user ID
    Given    an appropriate test context as detailed in the test data source,
    And    a user with a non-existing Idam user ID in CCD,
    When    a request is prepared with appropriate values,
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And    the response has all the details as expected.

  @S-511
  Scenario: must return 404 when no case found for the given Jurisdiction ID
    Given    an appropriate test context as detailed in the test data source,
    And    a user with a non-existing Jurisdiction ID in CCD,
    When    a request is prepared with appropriate values,
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And    the response has all the details as expected.

  @S-512
  Scenario: must return 404 when no case found for the given Case type ID
    Given    an appropriate test context as detailed in the test data source,
    And    a user with a non-existing Case type ID in CCD,
    When    a request is prepared with appropriate values,
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And    the response has all the details as expected.

  @S-513
  Scenario: must return 404 when no case found for the given Case ID
    Given    an appropriate test context as detailed in the test data source,
    And    a user with a non-existing Case ID in CCD,
    When    a request is prepared with appropriate values,
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And    the response has all the details as expected.

  @S-514
  Scenario: must return 404 when no case found for the given Event ID
    Given    an appropriate test context as detailed in the test data source,
    And    a user with a non-existing Event ID in CCD,
    When    a request is prepared with appropriate values,
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And    the response has all the details as expected.

  @S-234
  Scenario: must return 422 when process could not be started
    Given   an appropriate test context as detailed in the test data source,
    And     a user with an existing case in CCD
    When    a request is prepared with appropriate values,
    And     the request body can't be parsed
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then    a negative response is received,
    And     the response has all the details as expected.

  @S-231
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given   an appropriate test context as detailed in the test data source,
    And 	a user with an existing case in CCD
    When	a request is prepared with appropriate values,
    And     failed to provide any authentication credentials within the request.
    And	    it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then	a negative response is received,
    And 	the response has all the details as expected.

  @S-232
  Scenario: must return 403 when request provides authentic credentials without authorized access to the operation
    Given   an appropriate test context as detailed in the test data source,
    And 	a user with an existing case in CCD
    When	a request is prepared with appropriate values,
    And     a user Access Denied i.e. you don't have permission to access,
    And	    it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store],
    Then	a negative response is received,
    And 	the response has all the details as expected.
