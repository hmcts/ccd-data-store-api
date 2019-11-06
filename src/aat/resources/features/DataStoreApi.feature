Feature: DataStore API functionality
  Scenario: S129: must return default user settings successfully for a user having a profile in CCD
    Given: an appropriate test context as detailed in the test data source,
    And: a user with a detailed profile in CCD,
    When: a request is prepared with appropriate values,
    And: it is submitted to call the [Get default setting for user] operation of [CCD Data Store],
    Then: a positive response is received,
    And: 	the response has all the details as expected.

  Scenario: S130: must return appropriate negative response for a user not having a profile in CCD
    Given:  an appropriate test context as detailed in the test data source,
    And: a user with no profile in CCD,
    When: a request is prepared with appropriate values,
    And: it is submitted to call the [Get default setting for user] operation of [CCD Data Store],
    Then: a negative response is received,
    And: the response has all the details as expected.

