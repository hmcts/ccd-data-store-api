@F-050
Feature: Validate a set of fields as Case worker

  @S-301
  Scenario: must validate when all fields are valid
    Given an appropriate test context as detailed in the test data source
    And a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all the details as expected

#  @S-166
#  Scenario: must not validate when CMC ExternalID is not unique / already exists
#    Given an appropriate test context as detailed in the test data source
#    And a user with a detailed profile in CCD
#    When a request is prepared with appropriate values
#    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
#    Then a negative response is received
#    And the response has all the details as expected
#
#  @S-167
#  Scenario: must not validate when field validation fails
#    Given an appropriate test context as detailed in the test data source
#    And a user with a detailed profile in CCD
#    When a request is prepared with appropriate values
#    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store]
#    Then a negative response is received
#    And the response has all the details as expected
