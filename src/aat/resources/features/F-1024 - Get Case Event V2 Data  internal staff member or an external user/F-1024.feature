#==================================================================================
@F-1024
Feature: F-1024: Is CaseHistory event accessible to internal staff member and not accessible by external user
#==================================================================================

Background:
    Given an appropriate test context as detailed in the test data source

  #-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-1024.1
  Scenario: must return negative response for Specific Case Event Data by External Parties

    Given a user with [an active profile in CCD],

    And a successful call [to create a token for case creation] as in [Private_Autotest_Default_Token_Creation_Data_For_Case_Creation_External],
    And another successful call [to create a full case] as in [Private_Autotest_Case_Data_Extension_External],
    And another successful call [to get the details about case event for the case just created] as in [S-1024_Get_Private_Autotest_Case_Data_External],

    When a request is prepared with appropriate values,
    And the request [contains the reference of the case just created and the event id valid for that case],
    And it is submitted to call the [Retrieve a CaseView Event by case and event id for access to External Parties] operation of [CCD Data Store],

    Then a negative response is received,
    And the response [contains HTTP 403 Forbidden],
    And the response has all other details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-1024.2
Scenario: should retrieve case view with response code HTTP 200 when the case reference and case event exists response for authorised access by Internal Parties

    Given a user with [an active profile in CCD],

      And a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case] as in [Befta_Case_Data_Extension_Internal],
      And another successful call [to get the details about case event for the case just created] as in [S-1024_Get_Case_Data_Internal],

    When a request is prepared with appropriate values,
      And the request [contains the reference of the case just created and the event id valid for that case],
      And it is submitted to call the [Retrieve a CaseView Event by case and event id for access to Internal Parties] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

@Ignore
@S-1024.3
Scenario: must return negative response for unauthorised access by Internal Parties

    Given a user with [an active profile in CCD],

      And a successful call [to create a token for case creation] as in [Befta_Default_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case] as in [Befta_Case_Data_Extension_Read_Internal],
      And another successful call [to get the details about case event for the case just created] as in [S-1024_Get_Case_Data_Read_Internal],

      When a request is prepared with appropriate values,
      And the request [contains the reference of the case just created and the event id valid for that case],
      And it is submitted to call the [Retrieve a CaseView Event by case and event id for access to Internal Parties] operation of [CCD Data Store],


  Then a negative response is received,
      And the response [includes a HTTP 401 Unauthorised],
      And the response has all other details as expected.
