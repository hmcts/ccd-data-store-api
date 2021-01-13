@F-112 @elasticsearch @Ignore
Feature: Allow ignoring fields in ES by declaring them non searchable

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-112.1
  Scenario: No results returned when searching by non searchable top level field
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a case [with a non searchable 'Homeless' field] created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [is searching for the previously created case by the 'Homeless' field]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all the details as expected

  @S-112.2
  Scenario: No results returned when searching by non searchable complex child field
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a case [with a searchable Family complex but a non searchable 'MotherFullName' field] created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [is searching for the previously created case by the 'MotherFullName' field]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all the details as expected

  @S-112.4
  Scenario: No results returned when searching by non searchable field within a collection
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a case [with a searchable collection of Child complex but a non searchable 'ChildFullName' field] created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [is searching for the previously created case by the 'ChildFullName' field]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the response [contains no cases]
    And the response has all the details as expected

  @S-112.3
  Scenario: results returned when searching by searchable complex child field
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-112.3-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a case [with a searchable Family complex and a searchable 'FatherFullName' field] created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [is searching for the previously created case by the 'FatherFullName' field]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the response [contains the previously created case]
    And the response has all the details as expected

  @S-112.5
  Scenario: results returned when searching by searchable field within a collection
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [F-112.5-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a case [with a searchable collection of Child complex containing a searchable 'ChildFullName' field] created as in [F-112-Befta_Master_ComplexCollectionComplex_Case_Creation]
    And a wait time of [5] seconds [to allow for Logstash to index the case just created]
    When a request is prepared with appropriate values
    And the request [is searching for the previously created case by the 'ChildFullName' field]
    And it is submitted to call the [/searchCases] operation of [CCD Data Store api]
    Then a positive response is received
    And the response [contains the previously created case]
    And the response has all the details as expected

