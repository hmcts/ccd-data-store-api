#==============================================
@F-New
Feature: F-New: Retain Hidden Value
#==============================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-----------------------------------------------------------------------------------------------------------------------
  @S-New.1 # 'retainHiddenValue' set to 'Yes' or 'Y' on the config for top Level field
    # 'retainHiddenValue' set to 'No' or 'N' on the config for top Level field
      # 'retainHiddenValue' set to null on the config for top Level field
  Scenario: Must return status 200 along with successfully display the imported definition file containing a top level fields with the correct value for retainHiddenValue

    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.


#-----------------------------------------------------------------------------------------------------------------------
  @S-New.2 # 'retainHiddenValue' set to 'Yes' or 'Y' on the config for complex field
    # 'retainHiddenValue' set to 'No' or 'N' on the config for complex field
      # 'retainHiddenValue' set to null on the config for complex field
  Scenario: Must return status 200 along with successfully display the imported definition file containing complex fields with the correct value for retainHiddenValue

    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-----------------------------------------------------------------------------------------------------------------------
  @S-New.2 # 'retainHiddenValue' set to 'Yes' or 'Y' on the config for eventToComplex field
    # 'retainHiddenValue' set to 'No' or 'N' on the config for eventToComplex field
      # 'retainHiddenValue' set to null on the config for eventToComplex field
  Scenario: Must return status 200 along with successfully display the imported definition file containing eventToComplex fields with the correct value for retainHiddenValue

    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-----------------------------------------------------------------------------------------------------------------------
