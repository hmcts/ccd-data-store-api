@F-044
Feature: F-044: Submit event creation as Case worker

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-278
  Scenario: must submit the event creation successfully for correct inputs
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [Standard_Full_Case_Creation_Data]
    And a successful call [to get an event token for just created case] as in [S-044-Prerequisite]
    When a request is prepared with appropriate values
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a positive response is received
    And the response [includes the updated case details, along with a HTTP 201 code Created]
    And the response has all other details as expected

  @S-279 @Ignore
  Scenario: must return negative response when request does not provide a valid authentication credentials
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide valid authentication credentials]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-280 @Ignore
  Scenario: must return negative response when request does not provide an authorised access
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [does not provide authorised access to the operation]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [has the 403 return code]
    And the response has all other details as expected

  @S-281 @Ignore
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing jurisdiction ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-403]
    And the response has all the details as expected

  @S-282 @Ignore
  Scenario: must return 404 when request contains a non-existing Case type ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing Case type ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

  @S-283 @Ignore
  Scenario: must return 404 when request contains a non-existing Event ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing Event ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected

  @S-552 @Ignore
  Scenario: must return 404 when request contains a non-existing jurisdiction ID
    Given a user with [an active profile in CCD]
    When a request is prepared with appropriate values
    And the request [contains a non-existing jurisdiction ID]
    And it is submitted to call the [Submit event creation as Case worker] operation of [CCD Data Store]
    Then a negative response is received
    And the response [code is HTTP-404]
    And the response has all the details as expected


    @S-279 @Ignore
    Scenario: must return 401 when request does not provide valid authentication credentials
    <>

    @S-280 @Ignore
    Scenario: must return 403 when request provides authentic credentials without authorised access to the operation
    <>

    @S-277 @Ignore
    Scenario: <More tests out of further analysis>
    <>

    @S-278 @Ignore
    Scenario: must return 201 and Case Details object when Jurisdiction ID, case type ID and case id is valid
    <>

    @S-281 @Ignore
    Scenario: must return 404 when case is not found
    <>

    @S-282 @Ignore
    Scenario: must return 409 when case is altered out of the transaction
    <>

    @S-283 @Ignore
    Scenario: must return 422 when event submission has failed
    <>

    @S-113 @Ignore
    Scenario: should not update a case if the caseworker has 'C' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-114 @Ignore
    Scenario: should not update a case if the caseworker has 'CR' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-115 @Ignore
    Scenario: should not update a case if the caseworker has 'D' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-116
    Scenario: should not update a case if the caseworker has 'R' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-117
    Scenario: should progress case state
    <already implemented previously. will be refactored later.>

    @S-118
    Scenario: should update a case if the caseworker has 'CRUD' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-119
    Scenario: should update a case if the caseworker has 'CU' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-120
    Scenario: should update a case if the caseworker has 'RU' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-121
    Scenario: should update a case if the caseworker has 'U' access on CaseType
    <already implemented previously. will be refactored later.>

    @S-122
    Scenario: should update a single case field
    <already implemented previously. will be refactored later.>

