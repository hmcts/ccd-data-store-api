#====================================
@F-142
Feature: F-142: Get Linked Case V2 External
#====================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC01 - Case reference not supplied, Return 404 error
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.1
Scenario: should get 400 when case reference invalid

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid case reference],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC02 - Case reference supplied but does not exist in database, Return 404 error
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.2
Scenario: should get 404 when case reference does not exist

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a case reference that does not exist],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC03 - Case reference exists but user does not have access to the case, Return 404 error
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.3
Scenario: must return 404 when request provides authentic credentials without authorised access to the operation

    Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a valid user authorisation token without access to the operation],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Not Found],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC04 - startRecordNumber supplied is non-numeric, Return 400 error
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.4
Scenario: should get 400 when case reference is valid but startRecordNumber is invalid

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],

     When a request is prepared with appropriate values,
      And the request [contains valid case reference but invalid startRecordNumber],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC05 - maxReturnRecordCount supplied is non-numeric, Return 400 error
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.5
Scenario: should get 400 when case reference is valid but maxReturnRecordCount is invalid

    Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],

     When a request is prepared with appropriate values,
      And the request [contains valid case reference but invalid maxReturnRecordCount],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-400 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: AC06 - All valid details supplied and case exists, Return 200 success
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.6
Scenario: should retrieve case when the case reference exists

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 1 standard linked case with basic access for test account] as in [F-142_Create1LinkedCases_1StandardCaseLinks]
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for the linked case using the the new standard CaseLinks top level collection field],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13138: extra
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.7
Scenario: should return 404 when case reference is not supplied

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not contain a case reference],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains an HTTP-404 Bad Request],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC01 - Case Links don't exist for the supplied case reference, return 200 with an empty response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.8
Scenario: Case Links don't exist for the supplied case reference, return 200 with an empty response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [does not contain any case links for the given case reference],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC02 - No pagination parameters, case link exist for the case reference, all case links are in the standard field, return 200 with all the case links in response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.9
Scenario: No pagination parameters, case link exist for the case reference, all case links are in the standard field, return 200 with all the case links in response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 4 standard linked case with basic access for test account] as in [F-142_Create4LinkedCases_4StandardCaseLinks]
      And a user with [an active profile in CCD and basic access to all the linked cases created],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for all the linked cases using the the new standard CaseLinks top level collection field],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC03 - No pagination parameters, 5 case link exist for the case reference, all case links are in the standard field but user does not have access to 2 linked cases, return 200 with 3 case links in response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.10
Scenario: No pagination parameters, 5 case link exist for the case reference, all case links are in the standard field but user does not have access to 2 linked cases, return 200 with 3 case links in response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 5 standard linked case, 2 with no access for the test account] as in [F-142_Create5LinkedCases_5StandardCaseLinks_2WithNoAccess]
      And a user with [an active profile in CCD and basic access to only 3 of the linked cases created],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for only 3 of the linked cases using the the new standard CaseLinks top level collection field],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC04 - No pagination parameters, 8 case link exist for the case reference, 3 case links are not in the standard field, return 200 with 5 case links in response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.11
Scenario: No pagination parameters, 8 case link exist for the case reference, 3 case links are not in the standard field, return 200 with 5 case links in response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 5 standard linked case and 3 non standard case links, with basic access for test account] as in [F-142_Create8LinkedCases_5StandardCaseLinks_3NonStandardCaseLinks]
      And a user with [an active profile in CCD and basic access to all the linked cases created],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for the 5 linked cases using the the new standard CaseLinks top level collection field],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC05 - startRecordNumber = 1, maxReturnRecordsCount = 5, 13 case link exist for the case reference, 3 case links are not in the standard field, return 200 with 5 case links in response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.12
Scenario: startRecordNumber = 1, maxReturnRecordsCount = 5, 13 case link exist for the case reference, 3 case links are not in the standard field, return 200 with 5 case links in response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 10 standard linked case and 3 non standard case links, with basic access for test account] as in [F-142_Create13LinkedCases_10StandardCaseLinks_3NonStandardCaseLinks]
      And a user with [an active profile in CCD and basic access to all the linked cases created],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for the first 5 linked cases using the the new standard CaseLinks top level collection field with the hasMoreRecords attribute as "true"],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# RDM-13139: AC06 -  Continue from AC05, startRecordNumber = 6, maxReturnRecordCount = 5, return 200 with 5 case links in response payload
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-142.13
Scenario: Continue from AC05, startRecordNumber = 6, maxReturnRecordCount = 5, return 200 with 5 case links in response payload

    Given a successful call [to create the test case] as in [F-142_CreateTestCase_BeftaCaseType11],
      And a successful call [to create 10 standard linked case and 3 non standard case links, with basic access for test account] as in [F-142_Create13LinkedCases_10StandardCaseLinks_3NonStandardCaseLinks]
      And a user with [an active profile in CCD and basic access to all the linked cases created],

     When a request is prepared with appropriate values,
      And the request [contains the case reference of the case just created],
      And it is submitted to call the [retrieve linked cases by case reference] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains HTTP 200 Ok status code],
      And the response [contains the case link details for the next 5 linked cases using the the new standard CaseLinks top level collection field with the hasMoreRecords attribute as "false"],
      And the response has all other details as expected.
