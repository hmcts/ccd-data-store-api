@F-065
  Feature: F-065: Retrieve a case by ID for dynamic display

    Background: Load test data for the scenario
      Given an appropriate test context as detailed in the test data source

    @S-165
    Scenario: must return case view when the case reference exists
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [contains a case that has just been created as in Standard_Full_Case_Creation_Data]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains details of the case just created, along with an HTTP-200 OK]
      And the response has all other details as expected

    @S-164
    Scenario: must return case view history when the case reference exists
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      And a successful call [to get an event token for just created case] as in [S-164-Prerequisite]
      And another successful call [to update case with the token just created] as in [S-164-Prerequisite_Case_Update]
      When a request is prepared with appropriate values
      And the request [contains a case that has just been created as in Standard_Full_Case_Creation_Data]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains details of the case just created, along with an HTTP-200 OK]
      And the response [contains the case view history]
      And the response has all other details as expected

    @S-163 @Ignore #Defect submitted RDM-6868
    Scenario: must return 404 when case reference does NOT exist
      Given a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses case-reference which is not exist in CCD]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-404 code]
      And the response has all other details as expected

    @S-162
    Scenario: must return 400 when case reference is invalid
      Given a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses an invalid case-reference]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-400 code]
      And the response has all other details as expected

    @S-161 @Ignore # re-write as part of RDM-6847
    Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [does not provide an authorised access to the operation]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [contains an HTTP-403 Forbidden]
      And the response has all other details as expected

    @S-160 @Ignore # defect RDM-6628
    Scenario: must return 401 when request does not provide valid authentication credentials
      Given a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [does not provide valid authentication credentials to the operation]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-401 Unauthorized]
      And the response has all other details as expected

    @S-585
    Scenario: must return the case view with only the respective fields with READ access for Solicitors
      Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
      And a user with [another active Solicitor profile with different variation of ACLs on the same case type]
      And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
      And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
      When a request is prepared with appropriate values
      And the request [is prepared on behalf of the privileged user creating the case above]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains values for all fields under the case type for Solicitor 2]
      And the response has all other details as expected
      And a call [to get the same case by Solicitor 1] will get the expected response as in [F-065_Later_Case_Read_By_Solicitor_1]
      And a call [to get the same case by Solicitor 3] will get the expected response as in [F-065_Later_Case_Read_By_Solicitor_3]

    @S-586
    Scenario: must return the case view having only the respective fields with READ access when case is created by a less privileged Solicitor
      Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
      And a user with [another active Solicitor profile with different variation of ACLs on the same case type]
      And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation_Solicitor_1]
      And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data_solicitor_1]
      When a request is prepared with appropriate values
      And the request [is prepared on behalf of the user creating the case above]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains values for all fields under the case type for Solicitor 1]
      And the response has all other details as expected
      And a call [to get the same case by Solicitor 3] will get the expected response as in [F-065_Later_Case_Read_By_Solicitor_3_Generated_By_Solicitor_1]


