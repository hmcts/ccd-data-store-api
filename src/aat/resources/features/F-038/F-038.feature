#=====================================================
@F-038
Feature: F-038: Validate field removal restrictions when submitting an event for an existing case (V2)
#=====================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-038.1
Scenario: must remove simple fields from AddressUKField if not sent in the event data, unless Read=False and Create=True

  Given a user with [an active profile in CCD],
      And a case that has just been created as in [Standard_Full_Case_Creation_Data],
      And a successful call [to get an event token for the case just created] as in [F-038-Base-Prerequisite],

     When a request is prepared with appropriate values,
      And the request [contains a case Id that has just been created as in Standard_Full_Case_Creation_Data],
      And the request [contains a token created as in F-038-Base-Prerequisite],
      And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store],

     Then a positive response is received,
      And the response [contains the case detail for the updated case, along with a HTTP 200 OK],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-038.2
Scenario: must remove complex AddressUKField if sent null in the event data, unless Read=False and Create=True

  Given a user with [an active profile in CCD],
    And a case that has just been created as in [Standard_Full_Case_Creation_Data],
    And a successful call [to get an event token for the case just created] as in [F-038-Base-Prerequisite],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created as in Standard_Full_Case_Creation_Data],
    And the request [contains a token created as in F-038-Base-Prerequisite],
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store],

    Then a positive response is received,
    And the response [contains the case detail for the updated case, along with a HTTP 200 OK],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-038.3
  Scenario: must remove collection if not sent in the event data, unless Read=False and Create=True

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [Standard_Full_Case_Creation_Data],
    And a successful call [to get an event token for the case just created] as in [F-038-Base-Prerequisite],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created as in Standard_Full_Case_Creation_Data],
    And the request [contains a token created as in F-038-Base-Prerequisite],
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store],

    Then a positive response is received,
    And the response [contains the case detail for the updated case, along with a HTTP 200 OK],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-038.4
  Scenario: must add simple fields from AddressForeignField if not sent in the event data, if Read=False and Create=True

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [F-038-Case-Creation-Data],
    And a successful call [to get an event token for the case just created] as in [F-038-Base-PrivateCaseWorker-Prerequisite],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created as in F-038-Case-Creation-Data],
    And the request [contains a token created as in F-038-Base-PrivateCaseWorker-Prerequisite],
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store],

    Then a positive response is received,
    And the response [contains the case detail for the updated case, along with a HTTP 201 OK],
    And the response has all other details as expected.
    And another call [to verify that the missing fields are unchanged] will get the expected response as in [F-038.VerifyMissingFieldsUnchanged]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-038.5
  Scenario: must add collection items from CollectionPermissionField if not sent in the event data, if Read=False and
  Create=True

    Given a user with [an active profile in CCD],
    And a case that has just been created as in [F-038-Case-Creation-Data],
    And a successful call [to get an event token for the case just created] as in [F-038-Base-PrivateCaseWorker-Prerequisite],

    When a request is prepared with appropriate values,
    And the request [contains a case Id that has just been created as in F-038-Case-Creation-Data],
    And the request [contains a token created as in F-038-Base-PrivateCaseWorker-Prerequisite],
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD data store],

    Then a positive response is received,
    And the response [contains the case detail for the updated case, along with a HTTP 201 OK],
    And the response has all other details as expected.
    And another call [to verify that the missing fields are unchanged] will get the expected response as in [F-038.VerifyMissingFieldsUnchanged]
