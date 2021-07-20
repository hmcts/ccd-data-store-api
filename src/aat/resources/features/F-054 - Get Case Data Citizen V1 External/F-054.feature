#===================================
@F-054
Feature: F-054: Get case for Citizen
#===================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-093 # must return 200 and list of case data for the given case id
Scenario: must return 200 and list of case data for the given case id

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],

     When a request is prepared with appropriate values,
      And the request [contains case data matching the case just created above],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [code is HTTP-200],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-094 @Ignore # Response code mismatch, expected: 401, actual: 403 (defect RDM-6628)
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an invalid user authorisation token],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-401],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-095 @Ignore # as discussed we are going to implement the generic test for this
Scenario: must return 403 when request provides authentic credentials without authorized access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains an valid user authorisation token that does not have access to the operation],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-562 @Ignore # Response code mismatch, expected: 404, actual: 200 /RDM-7066
Scenario: must return negative response HTTP-404 when request contains a non-existing jurisdiction ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a non-existing jurisdiction ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-563 @Ignore # Response code mismatch, expected: 404, actual: 200 /RDM-7066
Scenario: must return negative response HTTP-404 when request contains a non-existing case type ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a non-existing case type ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-564
Scenario: must return negative response HTTP-404 when request contains a non-existing case reference ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a non-existing case reference ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-404],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-565
Scenario: must return negative response HTTP-403 when request contains a non-existing user ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a non-existing user ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-566 @Ignore # Response code mismatch, expected: 400, actual: 200 / RDM-7066
Scenario: must return negative response HTTP-400 when request contains a malformed jurisdiction ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a malformed jurisdiction ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-567 @Ignore # Response code mismatch, expected: 400, actual: 200 / RDM-7066
Scenario: must return negative response HTTP-400 when request contains a malformed case type ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a malformed case type ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-568
Scenario: must return negative response HTTP-400 when request contains a malformed case reference ID

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains a malformed case reference ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-400],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-569
Scenario: must return negative response HTTP-403 when request contains a malformed user ID

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],
      And another successful call [to create a full case as a citizen] as in [Citizen_Full_Case_Creation_Data],
      And a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [contains the ID of above created case with a malformed user ID],
      And it is submitted to call the [get case for citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [code is HTTP-403],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
