@F-1029
Feature: F-1029: Validate RichTextArea base type accepts and displays valid tags and rejects invalid tags

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-1029.1
  Scenario: A new case is created with RichTextArea base type with valid tags
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [S-1029.1_Create_Case_Data]
    When a request is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the request [RichTextArea has tags that are allowed list]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    Then a positive response is received
    And the response has all other details as expected

  @S-1029.2
    Scenario: A new case is not created with RichTextArea base type with invalid tags
    Given a user with [an active profile in CCD]
    And a case that has just been created as in [S-1029.2_Create_Case_Data]
    When a request is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the request [RichTextArea has tags that are outside of allowed list]
    And it is submitted to call the [Submit Case Creation as Caseworker] operation of [CCD Data Store]
    And the response [has the 400 Bad Request]
    Then a negative response is received
#    And error message is return [Enter valid tags for RichTextArea field]
    And the response [contains an error message : Enter valid tags for RichTextArea field],

  @S-1029.3
    Scenario: a case updated with RichTextArea base type with valid tags
    Given a user with [an active profile in CCD]
    When a request to create a new case is prepared with appropriate values
    And the request [contains correctly configured event details]
    And the request [contains RichTextArea base type]
    And the request [RichTextArea has tags that are allowed list]
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
    Then a negative response is received
    And Error message is return [Enter valid tags for RichTextArea field and case is not updated]
