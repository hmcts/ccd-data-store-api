@F-1029
Feature: F-1029: Validate RichTextArea base type accepts and displays valid tags and rejects invalid tags

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#  @S-1029.1
#  Scenario: must successfully create case with number field displaying as expected
#    Given a user with [an active profile in CCD]
#    And a case that has just been created as in [S-1029.1_Create_Case_Data]
#    And a successful call [to create a token for case creation] as in [F-1029_Case_Data_Create_Token_Creation]
#    When a request is prepared with appropriate values
#    And the request [is of caseType where case_data has NumberField of 12.88]
#    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
#    Then a positive response is received
#    And the response has all other details as expected


  @S-1029.1
  Scenario: A new case is created with RichTextArea base type with valid tags
    Given a user with [an active profile in CCD]
    And a successful call [to create a token for case creation] as in [S-1029.1_Create_Case_Data_Token_Creation]
    When a request is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the [RichTextArea has tags that are allowed list]
    And it is submitted to [create a new case] operation of [CCD Data Store API]
    And the response [has the 201 CREATED code]
    Then a positive response is received
    And the response has all other details as expected and a new case is created


  @S-1029.2
    Scenario: A new case is not created with RichTextArea base type with invalid tags
    Given a user with [an active profile in CCD]
    When a request to create a new case is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the [RichTextArea has tags that are outside of allowed list]
    And it is submitted to [create a new case] operation of [CCD Data Store API]
    And the response [has the 400 Bad Request]
    Then a Negative response is received
    And Error message is return [Enter valid tags for RichTextArea field]

  @S-1029.3
    Scenario: a case with RichTextArea base type with valid tags
    Given a user with [an active profile in CCD]
    When a request to create a new case is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the [RichTextArea has tags that are allowed list]
    And it is submitted to [create a new case] operation of [CCD Data Store API]
    And the response [has the 200 OK code]
    Then a positive response is received
    And the response has all other details as expected and a case is updated


  @S-1029.4
  Scenario: Edit a case with created with RichTextArea base type with invalid tags
    Given a user with [an active profile in CCD]
    When a request to create a new case is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the [RichTextArea has tags that are outside of allowed list]
    And it is submitted to [create a new case] operation of [CCD Data Store API]
    And the response [has the 400 Bad Request]
    Then a Negative response is received
    And Error message is return [Enter valid tags for RichTextArea field and case is not updated]
