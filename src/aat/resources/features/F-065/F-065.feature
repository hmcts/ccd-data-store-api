@F-065
  Feature: F-065: Retrieve a case by ID for dynamic display

    Background: Load test data for the scenario
      Given an appropriate test context as detailed in the test data source

    @S-165
    Scenario: must return case view when the case reference exists
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses case-reference of the case just created]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains details of the case just created, along with an HTTP-200 OK]
      And the response has all other details as expected

    @S-164 @Ignore #RDM-6874
    Scenario: must return case view history when the case reference exists
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses case-reference of the case just created]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a positive response is received
      And the response [contains details of the case just created, along with an HTTP-200 OK]
      And the response [contains the case view history]
      And the response has all other details as expected

    @S-163
    Scenario: must return 404 when case reference does NOT exist
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses case-reference which is not exist in CCD]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-404 code]
      And the response has all other details as expected

    @S-162
    Scenario: must return 400 when case reference is invalid
      Given a case that has just been created as in [Standard_Full_Case_Creation_Data]
      And a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [uses an invalid case-reference]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-400 code]
      And the response has all other details as expected

    @S-161
    Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
      Given a user with [an active profile in CCD]
      When a request is prepared with appropriate values
      And the request [does not provide valid authentication credentials]
      And it is submitted to call the [Retrieve a case by ID for dynamic display] operation of [CCD Data Store]
      Then a negative response is received
      And the response [has an HTTP-403 code]
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
