@F-032
Feature: F-032: Get printable documents

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-124
  Scenario: must retrieve printable documents successfully for correct inputs
    Given a case that has just been created as in [F-032_Case_Creation_Data_With_Document]
    And a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains the case data of the case just created]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a link to the printable documents that were uploaded to the case just created, along with a HTTP 200 OK]
    And the response has all other details as expected

  @S-125 @Ignore #defect RDM-6628
  Scenario: must return 401 when request does not provide valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains an invalid user authentication token]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 401 Unauthorised]
    And the response has all other details as expected

  @S-126 @Ignore # expected 403 but got 200 (defect RDM-6881)
  Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    Given a case that has just been created as in [S-126_Superuser_Case_Creation_Data_With_Document]
    And a user with [an active profile in CCD but without read access to the case just created]
    When a request is prepared with appropriate values
    And the request [contains the case data of the case just created]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 403 Forbidden]
    And the response has all other details as expected

  @S-127 @Ignore # expected 404 but got 200 (defect RDM-6881)
  Scenario: must return 404 for non-existing case type id
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing case type id]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 404 Not Found]
    And the response has all other details as expected

  @S-128 @Ignore # expected 404 but got 200 (defect RDM-6881)
  Scenario: must return 404 for non-existing jurisdiction id
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing jurisdiction id]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a negative response is received
    And the response [contains a HTTP 404 Not Found]
    And the response has all other details as expected

  @S-123
  Scenario: must return successfully all and only the respective documents with read access for solicitors
    Given a user with [an active solicitor 1 profile in CCD with read permissions on limited documents for a given case type]
    And a user with [another active solicitor 2 profile in CCD with all permissions on all documents for a given case type]
    And a user with [another active solicitor 3 profile in CCD with read permissions on different limited documents for a given case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [is made by solicitor 2 who just created the case]
    And it is submitted to call the [get printable documents] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains a link to all of the printable documents that were uploaded to the case just created, along with a HTTP 200 OK]
    And the response has all other details as expected
    And a call [to get the same case by solicitor 1] will get the expected response as in [F-032_Case_Read_By_Solicitor_1]
    And a call [to get the same case by solicitor 3] will get the expected response as in [F-032_Case_Read_By_Solicitor_3]
