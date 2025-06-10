#===========================================
@F-129 @access-control
Feature: F-129: Access Control tests covering Security Classification feature
#===========================================

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-129.1
  Scenario: User with PUBLIC SC role access can access fields with a Security Classification of PRIVATE or RESTRICTED since field level classification is disabled

    And a user with [idam roles which only have a max Security Classification of PUBLIC]
    And a case that has just been created as in [SC_Public_Case_Created],


    When a request is prepared with appropriate values,
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],


    Then a positive response is received,
    And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-129.4
  Scenario: User with PUBLIC SC role access cannot access cases with a Security Classification of PRIVATE or RESTRICTED


    And a user with [idam roles which only have a max Security Classification of PUBLIC]
    And a case that has just been created as in [SC_Public_Case_Created],
    And a case that has just been created as in [SC_Private_Case_Created],
    And a case that has just been created as in [SC_Restricted_Case_Created],


    When a request is prepared with appropriate values,
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],
    And the request [tries to retrieve the PUBLIC SC case that was previously created]


    Then a positive response is received
    And the response has all the details as expected
    And the response [returns the case]
    And a call [to retrieve the Private SC case] will get the expected response as in [F-129_Private_Case_Not_Found].
    And a call [to retrieve the Restricted SC case] will get the expected response as in [F-129_Restricted_Case_Not_Found].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-129.5
  Scenario: User with PRIVATE SC role access get a case with a Security Classification of Private and PRIVATE but not RESTRICTED

    And a user with [idam roles which only have a max Security Classification of PRIVATE]
    And a case that has just been created as in [SC_Public_Case_Created],
    And a case that has just been created as in [SC_Private_Case_Created],
    And a case that has just been created as in [SC_Restricted_Case_Created],


    When a request is prepared with appropriate values,
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],
    And the request [tries to retrieve the PUBLIC SC case that was previously created]


    Then a positive response is received


    And the response has all the details as expected
    And the response [returns the case]
    And a call [to retrieve the Private SC case] will get the expected response as in [F-129.5_Private_Case_Found].
    And a call [to retrieve the Restricted SC case] will get the expected response as in [F-129.5_Restricted_Case_Not_Found].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-129.6
  Scenario: User with RESTRICTED SC role access can access cases with a Security Classification of PUBLIC, PRIVATE and RESTRICTED

    And a user with [idam roles which only have a max Security Classification of RESTRICTED]
    And a case that has just been created as in [SC_Public_Case_Created],
    And a case that has just been created as in [SC_Private_Case_Created],
    And a case that has just been created as in [SC_Restricted_Case_Created],


    When a request is prepared with appropriate values,
    And it is submitted to call the [retrieve a case by id] operation of [CCD Data Store],
    And the request [tries to retrieve the PUBLIC SC case that was previously created]


    Then a positive response is received


    And the response has all the details as expected
    And the response [returns the case]
    And a call [to retrieve the Private SC case] will get the expected response as in [F-129_Private_Case_Found].
    And a call [to retrieve the Restricted SC case] will get the expected response as in [F-129_Restricted_Case_Found].

