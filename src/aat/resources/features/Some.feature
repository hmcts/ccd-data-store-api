Feature:
  Scenario: some scenario
    Given an appropriate test context as detailed in the test data source
    And a user with a detailed profile in CCD
    And they have a judge role
    When a request is prepared with appropriate values
    And it is submitted to call the get default settings for user operation of CCD Data Store
    Then a positive response is received
    And the response has all the details as expected
