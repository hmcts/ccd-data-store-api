@F-058
Feature: Start case creation as Citizen

  @S-237
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [does not provide valid authentication credentials in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-238
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [uses a provides authentic credentials without authorized access to the operation in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-239
  Scenario: must return 404 when no case found for the given Idam user ID
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [uses a non-existing Idam user ID in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-240
  Scenario: must return 422 when process could not be started
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [body can't be parsed]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-241
  Scenario: must start case creation process successfully for correct inputs
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a positive response is received
    And     the response has all the details as expected

  @S-517
  Scenario: must return 404 when no case found for the given Jurisdiction ID
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [uses a non-existing Jurisdiction ID in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-518
  Scenario: must return 404 when no case found for the given Case type ID
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [uses a non-existing Case type ID in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-519
  Scenario: must return 404 when no case found for the given Case ID
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [a non-existing Case ID in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-520
  Scenario: must return 404 when no case found for the given Event ID
    Given   an appropriate test context as detailed in the test data source
    And     a user with [an existing case in CCD]
    When    a request is prepared with appropriate values
    And     the request [a non-existing Event ID in CCD]
    And     it is submitted to call the [Start case creation as Case worker] operation of [CCD Data Store]
    Then    a negative response is received
    And     the response has all the details as expected

  @S-236
  Scenario: <More tests out of further analysis>

