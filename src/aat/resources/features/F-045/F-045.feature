@F-045
Feature: Grant access to case

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-151
  Scenario: must return 201 if the grant is successful for a user to a valid case ID
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [has a valid case-reference in input parameters]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 201 return code]
    And the response has all the details as expected

  @S-152
  Scenario: must return 404 if case id is invalid
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [doesn't provide a valid case-reference in input parameters]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 404 return code]
    And the response has all the details as expected

  @S-153
  Scenario: must return negative response when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the valid authentication credentials]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-154
  Scenario: must return negative response when request provides authentic credentials without authorized access to the operation
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the authorised access to the operation]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all the details as expected

  @S-544
  Scenario: must return negative response when request body doesn't provide the mandatory field
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the mandatory field for the operation]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all the details as expected

  @S-545
  Scenario: must return negative response when case id contains some non-numeric characters
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide the numeric case id for the operation]
    And it is submitted to call the [Grant access to case] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 500 return code]
    And the response has all the details as expected
