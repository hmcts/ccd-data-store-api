@F-040
Feature: F-040: Get Case for Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-576
  Scenario: must return successfully all and only the respective fields with READ access for Solicitors
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a user with [another active solicitor profile with different variation of ACLs on the same case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [is prepared on behalf of the user creating the case above]
    And it is submitted to call the [Get Case for Case Worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains values for all fields under the case type]
    And the response has all other details as expected
    And a call [to get the same case by Solicitor 1] will get the expected response as in [F-040_Later_Case_Read_By_Solicitor_1]
    And a call [to get the same case by Solicitor 3] will get the expected response as in [F-040_Later_Case_Read_By_Solicitor_3]
