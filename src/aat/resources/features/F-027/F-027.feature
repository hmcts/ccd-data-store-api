@F-027
Feature: Fetch a case for display for Case Worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-038
  Scenario: must return status 200 along with the case-view object successfully
    Given a user with [a detailed profile in CCD]
    And a case that has just been created as in [Standard_Full_Case]
    When a request is prepared with appropriate values
    And the request [contains as input a valid case-reference that exists in CCD]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [has the 200 return code]
    And the response has all other details as expected

  @S-035
  Scenario: must return appropriate negative response when case id is structurally valid but not exist in CCD
    Given a user with [a detailed profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains in input parameters a structurally valid but non-existing case-reference]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 400 return code]
    And the response has all other details as expected

  @S-036
  Scenario: must return appropriate negative response when request does not provide valid authentication credentials
    Given a user with [a detailed profile in CCD]
    And a case that has just been created as in [Standard_Full_Case]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-037
  Scenario: must return appropriate negative response when request does not provide an authorized access
    Given a user with [a detailed profile in CCD]
    And a case that has just been created as in [Standard_Full_Case]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-034
  Scenario: must return appropriate negative response for a user not having a profile in CCD
    Given a user with [no profile in CCD]
    And a case that has just been created as in [Standard_Full_Case]
    When a request is prepared with appropriate values
    And it is submitted to call the [Fetch a case for display for Case Worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected
