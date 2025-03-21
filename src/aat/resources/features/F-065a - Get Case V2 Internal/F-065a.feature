#==========================================================
@F-065a
  Feature: F-065a: Retrieve access metadata for a given case ID
#==========================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @S-065a.1
    Scenario: must return case access metadata when the case reference exists

      Given a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],
      And a successful call [to get an event token for just created case] as in [S-065a-Prerequisite],
      And another successful call [to update case with the token just created] as in [S-065a-Prerequisite_Case_Update],

      When a request is prepared with appropriate values,
      And the request [contains a case that has just been created as in Standard_Full_Case_Creation_Data],
      And it is submitted to call the [Retrieve access metadata for a given case ID] operation of [CCD Data Store],

      Then a positive response is received,
      And the response [contains details of the case just created, along with an HTTP-200 OK],
      And the response [contains the case access metadata],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @S-065a.2
    Scenario: must return 404 when case reference does NOT exist

      Given a user with [an active profile in CCD],

      When a request is prepared with appropriate values,
      And the request [uses case-reference which does not exist in CCD],
      And it is submitted to call the [Retrieve access metadata for a given case ID] operation of [CCD Data Store],

      Then a positive response is received,
      And the response [has an HTTP-200 OK],
      And the response [contains the case access metadata with null],

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @S-065a.3
    Scenario: must return 400 when case reference is invalid

      Given a user with [an active profile in CCD],

      When a request is prepared with appropriate values,
      And the request [uses an invalid case-reference],
      And it is submitted to call the [Retrieve access metadata for a given case ID] operation of [CCD Data Store],

      Then a negative response is received,
      And the response [has an HTTP-400 code]
