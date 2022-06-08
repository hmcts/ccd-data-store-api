@F-140
Feature: F-140: CategoriesAndDocument endpoint

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
    And a successful call [to upload a document] as in [F-140_Document_Upload],

  @S-140.1 #AC1
  Scenario: Invalid case reference supplied (Non-extant) - Return 404 error
    Given a user with [an active profile in CCD],
    When a request is prepared with appropriate values,
    And the request [contains a Non-extant case reference in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 "001 Non-extant case"],
    And the response has all other details as expected.

  @S-140.2 #AC2
  Scenario: User doesn't have access to the supplied case reference - Return 404 error
    Given a case that has just been created as in [S-140.2_CreateCaseNoAccess],
    And a user with [an active profile in CCD and no access to case C1],
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a negative response is received,
    And the response [contains a HTTP 404 "002 Unauthorised for case"],
    And the response has all other details as expected.

  @S-140.3 #AC3
  Scenario: Categories defined, top-level document and document within complex types exist and User has access permissions - Hierarchy returned in the response
    Given a case that has just been created as in [F-140_CreateCase_hierarchyComplex],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    And [a case containing the above document fields *but without* the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy],
    And the response has all other details as expected.

  @S-140.4 #AC4
  Scenario:  Categories defined, top-level document and document within complex types exist and User has case access BUT not to the Document permissions
    Given a case that has just been created as in [F-140_CreateCase_hierarchyComplex],
    And a user with [an active profile in CCD and doesn't have read access permissions for most of the the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    And [a case containing the above document fields *but without* the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy with only Document2],
    And the response has all other details as expected.

  @S-140.5 #AC5
  Scenario:  Categories defined, top-level Collection of document and Collection of document within complex types exist and User has case access and the Document read permissions - Hierarchy with the list of documents is returned in the response
    Given a case that has just been created as in [F-140_CreateCase_hierarchyCollectionComplex],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Collection of Document fields in CaseField tab and ComplexTab exist with the category Id for case type CT1] in the context,
    And [a case C1, containing multiple document fields (as part of the Collection fields) *but without* the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy],
    And the response has all other details as expected.

  @S-140.6 #AC6
  Scenario: Categories defined, top-level Collection of document and Collection of document within complex types exist and User has case access BUT NO Document read permissions - Hierarchy without the list of documents is returned in the response
    Given a case that has just been created as in [F-140_CreateCase_hierarchyCollectionComplex],
    And a user with [an active profile in CCD and doesn't have read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Collection of Document fields in CaseField tab and ComplexTab exist with the category Id for case type CT1] in the context,
    And [a case C1, containing multiple document fields (as part of the Collection fields) *but without* the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy without the documents],
    And the response has all other details as expected.

  @S-140.7 #AC7
  Scenario: Top level document and document within complex type defined, both the document field types have non-null sub-field category_id, category hierarchy displayed with the category from the document sub-field
    Given a case that has just been created as in [F-140_CreateCase_hierarchyComplex_override],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Document fields in CaseField tab and ComplexTab exist with a category Id for case type CT1] in the context,
    And [a case C1, containing the above document fields with the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy],
    And the response has all other details as expected.

  @S-140.8 #AC8
  Scenario:  Collection of documents with some documents having sub-field category_id in the case - Hierarchy with the list of documents (displayed in the sub-field category_id wherever applicable) is returned in the response
    Given a case that has just been created as in [F-140_CreateCase_hierarchyCollectionComplex_override],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with category structure exists for the case type CT1] in the context,
    And [a case definition with Collection of Document fields in CaseField tab and ComplexTab exist with the category Id for case type CT1] in the context,
    And [a case C1, containing the above document fields with the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy],
    And the response has all other details as expected.

  @S-140.9 #AC9
  Scenario:  No Categories defined in Categories tab, no categories assigned to Document - Return the hierarchy with ALL documents in the "unCategorisedDocuments" element.
    Given a case that has just been created as in [F-140_CreateCase_NoCategories],
    And a user with [an active profile in CCD and has read access permissions for all the Document fields],
    And [a case definition with Document fields in CaseField tab and ComplexTab exist *without* the category Id for case type CT1] in the context,
    And [a case definition with Collection of Document fields in CaseField tab and ComplexTab exist with the category Id for case type CT1] in the context,
    And [a case C1, containing the above document fields *but without* the category_id in the Document type sub-field exists] in the context,
    When a request is prepared with appropriate values,
    And the request [contains the given case reference C1 in the input],
    And it is submitted to call the [Get categoriesAndDocuments] operation of [CCD Data Store],
    Then a positive response is received,
    And the response [contains a HTTP 200 status code],
    And the response [contains the category hierarchy],
    And the response has all other details as expected.
