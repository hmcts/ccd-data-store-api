#=================================================
@F-056
Feature: F-056: Submit Event Creation as a Citizen
#=================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-284 @Ignore # re-write as part of RDM-6847
Scenario: must return 401 when request does not provide valid authentication credentials

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide an authorized access to the operation],
      And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-285 @Ignore # re-write as part of RDM-6847
Scenario: must return 403 when request provides authentic credentials without authorized access to the operation

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And the request [does not provide a valid authentication credentials],
      And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 403 Forbidden],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-286 @Ignore
#    Code says "409" when case reference is not unique however we do not provide a case reference
#  Scenario is when the case has been altered outside the transaction, as the endpoint doesnt allow for case ref to be passed in this scenario
  #  to be ignored for now and investigated further later on
Scenario: must return 409 for a case that has been altered outside of transaction

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-287
Scenario: must return 422 when event submission fails

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],

     When a request is prepared with appropriate values,
      And the request [contains the token just generated and invalid case creation data],
      And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store],

     Then a negative response is received,
      And the response [contains a HTTP 422 Unprocessable Entity],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-288
Scenario: must return 201 when start event creation process for appropriate inputs

    Given a user with [an active profile in CCD],
      And a successful call [to create a token for case creation as a citizen] as in [Citizen_Token_Creation_Data_For_Case_Creation],

     When a request is prepared with appropriate values,
      And the request [contains a token created as in Citizen_Token_Creation_Data_For_Case_Creation],
      And it is submitted to call the [submit case creation as citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [includes the case detail for the updated case, along with a HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-584
Scenario: must update successfully the respective fields with ACL permissions for a Citizen

    Given a user with [an active citizen profile in CCD with update permissions for certain fields but not for others in a given case type],
      And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Citizen_Case_Creation],
      And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Citizen_Case_Creation_Data],
      And another successful call [to get an update event token for the case just created] as in [S-584-Prerequisite_Citizen_Token_For_Update_Case],

     When a request is prepared with appropriate values,
      And the request [is made to update the document metadata in DocumentField2, which the user has update permissions for],
      And it is submitted to call the [Submit event creation as a Citizen] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [contains updated values for DocumentField2],
      And the response has all other details as expected,
      And another successful call [to get an update event token for the case just created] as in [S-584-Prerequisite_Citizen_Token_For_Update_Case],
      And a call [to update the values for DocumentField4 from the same case, which the user does not have update permissions for] will get the expected response as in [S-584_Later_Case_Update_By_Citizen].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


