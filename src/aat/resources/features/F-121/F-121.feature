#======================================================
@F-121
Feature: F-121: Order Complex fields in start event
#======================================================

Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@S-121.1 @Ignore
Scenario: Must successfully display the ordering for an imported definition file containing a complex type with two address fields

    Given a user with [an active profile in CCD],

     When a request is prepared with appropriate values,
      And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

     Then a positive response is received,
      And the response [has the 200 return code],
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.2 @Ignore
  Scenario: Must display Complex types and Sub fields as they are ordered in the definition file

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.3 @Ignore
  Scenario: If no 'DisplayOrder' is defined in the definition file then must display Complex types and Sub fields in the order entered on the definition file

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.4
  Scenario: When EventToComplexTypes has mixed ordering of the sub fields (some defined and some not), Sub fields with no order must float to the bottom, if there is at least one subfield with the order defined on the same level.

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.5 @Ignore
  Scenario: CaseEventToFields mixed ordering should make elements with no ordering defined float to the bottom on Non leaf fields

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.6
  Scenario: EventToComplexTypes - Address fields must float to the bottom of the list when one field is added between the addresses

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-121.7
  Scenario: Position of the ordered address fields must change when one field is added between the ordered

    Given a user with [an active profile in CCD],

    When a request is prepared with appropriate values,
    And it is submitted to call the [validation of a set of fields as Case worker] operation of [CCD Data Store],

    Then a positive response is received,
    And the response [has the 200 return code],
    And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
