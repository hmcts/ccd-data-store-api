@F-141
Feature: F-141: DocumentData endpoint

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a successful call [to upload a document] as in [F-141_Document_Upload],

  @S-141.1 #AC1
  Scenario: Case reference supplied does not exist, Return 404 error
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a Non-extant case reference in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 status code],
    And the response has all other details as expected.

  @S-141.2 #AC2
  Scenario: User doesn't have access to the supplied case reference - Return 404 error
    Given a case that has just been created as in [S-141.2_CreateCaseNoAccess],
    And a user with [an active profile in CCD and no access to case C1],
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 status code],
    And the response has all other details as expected.

  @S-141.3 #AC3
  Scenario: Document field identified by the attributePath value does not exist in definition file, Return 400 - Bad request error
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains an attributePath value as "EvidenceDocument.document" (non existing in definition file) in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 400 status code],
    And the response has all other details as expected.

  @S-141.4 #AC4
  Scenario: Field specified in the attributePath value exists in definition file but it is not a Document type field, Return 400 - Non Document field
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains an attributePath value as "ApplicantName" (non Document Type field) in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 400 status code],
    And the response has all other details as expected.

  @S-141.5 #AC5
  Scenario: User does not have access to the document field supplied in the attributePath value, Return 404 error
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and doesn't have Update access permissions for the Document field named in the attributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains an attributePath value as "Document2"  in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 status code],
    And the response has all other details as expected.

  @S-141.6 #AC6
  Scenario: CategoryId is supplied but does not exist for the case type, Return 400 -002 Invalid CategoryId
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains CategoryID value as "CategoryID4" and attributePath value as "Document3" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 400 - 002 invalid category id],
    And the response has all other details as expected.

  @S-141.7 #AC7
  Scenario:  CaseVersion supplied does not match with CaseVersion in database, Return 400 - 003 - wrong case CaseVersion
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains a caseVersion that doesn't match with the one in the database],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 400 - 003 wrong case version],
    And the response has all other details as expected.

  @S-141.8 #AC8
  Scenario: Document with attributePath didn't have a category, now supplied a valid categoryId - return 200 response with the updated document hierarchy structure for the case
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains CategoryID value as "CategoryID2" and attributePath value as "Document3" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response has all other details as expected,
    And a call [to verify that the case is updated with category_id and sub-fields for Document3 and it is populated with "CategoryID2"] will get the expected response as in [S-141.8_GetCase],
    And a call [to verify that the Case Event History contains a new event called "DocumentUpdated"] will get the expected response as in [F-141_GetCaseEventHistory].

  @S-141.9 #AC9
  Scenario: Document with attributePath already had a category in definition file, now supplied a valid new categoryId - return 200 response with the updated document hierarchy structure for the case
    Given a case that has just been created as in [F-141_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains CategoryID value as "CategoryID1" and attributePath value as "Document2" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response has all other details as expected,
    And a call [to verify that the case is updated with category_id and sub-fields for Document2 and it is populated with "CategoryID1"] will get the expected response as in [S-141.9_GetCase],
    And a call [to verify that the Case Event History contains a new event called "DocumentUpdated"] will get the expected response as in [F-141_GetCaseEventHistory].

  @S-141.10 #AC10
  Scenario: Document with attributePath had sub-field category id existing, but also having categoryId in definition file, categoryId request value now supplied as Null - return 200 response with the updated document hierarchy (showing document in category as per definition file) for the case
    Given a case that has just been created as in [S-141.10_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains CategoryID value as "Null" and attributePath value as "Document2" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response has all other details as expected,
    And a call [to verify that the case is updated with category_id and sub-fields for Document2 and it is populated with "Null"] will get the expected response as in [S-141.10_GetCase],
    And a call [to verify that the Case Event History contains a new event called "DocumentUpdated"] will get the expected response as in [S-141.10_GetCaseEventHistory].

  @S-141.11 #AC11
  Scenario: Document with attributePath had sub-field category id existing but no default categoryID in definition file, now supplied Null categoryId in request - return 200 response with the updated document hierarchy (showing document as Uncategorized) for the case
    Given a case that has just been created as in [S-141.11_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And the request [contains CategoryID value as "Null" and attributePath value as "Document3" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response has all other details as expected,
    And a call [to verify that the case is updated with category_id and sub-fields for Document3 and it is populated with "Null"] will get the expected response as in [S-141.11_GetCase],
    And a call [to verify that the Case Event History contains a new event called "DocumentUpdated"] will get the expected response as in [S-141.11_GetCaseEventHistory].

  @S-141.12 #AC12
  Scenario: Document with attributePath had sub-field category id existing, now supplied new value - return 200 response with the updated document hierarchy for the case
    Given a case that has just been created as in [S-141.12_CreateCase],
    And a user with [an active profile in CCD and has Update access permissions for the Document field named in the AttributePath],
    And [a case definition with category structure exists for the case type CT2] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT2] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C2 in the input],
    And the request [contains CategoryID value as "CategoryID6" and attributePath value as "Document4" in the input],
    And it is submitted to call the [PUT documentData] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response has all other details as expected,
    And a call [to verify that the case is updated with category_id and sub-fields for Document4 and it is populated with "CategoryID6"] will get the expected response as in [S-141.12_GetCase],
    And a call [to verify that the Case Event History contains a new event called "DocumentUpdated"] will get the expected response as in [S-141.12_GetCaseEventHistory].
