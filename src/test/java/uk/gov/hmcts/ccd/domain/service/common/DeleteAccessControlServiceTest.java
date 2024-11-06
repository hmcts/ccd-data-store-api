package uk.gov.hmcts.ccd.domain.service.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DOCUMENT;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ACCESS_PROFILES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getTagFieldDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DeleteAccessControlServiceTest {

    static String complexTypeArrayPayload = """
        {
          "Note": {
            "Tags": [
              {
                "value": {
                  "Tag": "private",
                  "Category": "Personal"
                },
                "id": "123"
              },
              {
                "value": {
                  "Tag": "public",
                  "Category": "Work"
                },
                "id": "456"
              }
            ]
          }
        }
        """;
    static String arrayPayload = """
        {
            "Tags": [
              {
                "value": {
                  "Tag": "private",
                  "Category": "Personal"
                },
                "id": "123"
              },
              {
                "value": {
                  "Tag": "public",
                  "Category": "Work"
                },
                "id": "456"
              }
            ]
        }
        """;
    static String complexTypePayload = """
        {
          "Note": {
            "type": "PersonalNote",
            "metadata": {
              "authorName": "John Doe",
              "creationDate": "2024-11-04"
            },
            "content": {
              "title": "Meeting Notes",
              "body": "Discussion about project timelines and deliverables.",
              "additionalInfo": {
                "noteID": "abc123",
                "category": "Meeting",
                "tags": "project, timeline, deliverable"
              }
            },
            "location": {
              "AddressLine1": "123 Main Street",
              "AddressLine2": "Suite 500",
              "City": "Anytown",
              "State": "Anystate",
              "Country": "AnyCountry",
              "PostalCode": "12345"
            }
          }
        }
        """;

    static String nestedComplexTypeArrayPayload = """
        {
           "caseCategory": {
               "value": {
                   "code": "Test",
                   "label": "Test"
               },
               "list_items": [
                   {
                       "id": "123456",
                       "value": {
                           "code": "Test",
                           "label": "Test"
                       }
                   }
               ]
           }
        }
        """;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    private List<ILoggingEvent> loggingEventList;
    private DeleteAccessControlService service;

    @BeforeEach
    void setUp() {
        service = new DeleteAccessControlService();
    }

    private CaseFieldDefinition noteWithoutDeletePermission() {
        CaseFieldDefinition note = newCaseField()
            .withId("Note")
            .withFieldType(aFieldType()
                .withId("NoteComplex")
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                    .withId("Txt")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())
                .withComplexField(getTagFieldDefinition())
                .build())
            .build();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(note).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return note;
    }

    private CaseFieldDefinition noteWithDeletePermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setDelete(true);

        CaseFieldDefinition note = newCaseField()
            .withId("Note")
            .withAcl(deletePermission)
            .withFieldType(aFieldType()
                .withId("NoteComplex")
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                    .withId("Txt")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())
                .withComplexField(getTagFieldDefinition())
                .build())
            .build();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(note).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return note;
    }

    private CaseFieldDefinition tagsWithoutDeletePermission() {
        CaseFieldDefinition tags = getTagFieldDefinition();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(tags).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return tags;
    }

    private CaseFieldDefinition tagsWithDeletePermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setDelete(true);

        CaseFieldDefinition tags = getTagFieldDefinition();
        tags.setAccessControlLists(List.of(deletePermission));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(tags).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return tags;
    }

    private CaseFieldDefinition noteWithNestedFieldsWithoutDeletePermission() {
        CaseFieldDefinition note = newCaseField()
            .withId("Note")
            .withFieldType(aFieldType()
                .withId("NoteComplex")
                .withType(COMPLEX)

                .withComplexField(newCaseField()
                    .withId("type")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .build())

                .withComplexField(newCaseField()
                    .withId("metadata")
                    .withFieldType(aFieldType()
                        .withId("MetadataComplex")
                        .withType(COMPLEX)
                        .withComplexField(newCaseField()
                            .withId("authorName")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("creationDate")
                            .withFieldType(aFieldType()
                                .withId("Date")
                                .withType("Date")
                                .build())
                            .build())
                        .build())
                    .build())

                .withComplexField(newCaseField()
                    .withId("content")
                    .withFieldType(aFieldType()
                        .withId("ContentComplex")
                        .withType(COMPLEX)
                        .withComplexField(newCaseField()
                            .withId("title")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("body")
                            .withFieldType(aFieldType()
                                .withId("TextArea")
                                .withType("TextArea")
                                .build())
                            .build())

                        .withComplexField(newCaseField()
                            .withId("additionalInfo")
                            .withFieldType(aFieldType()
                                .withId("AdditionalInfoComplex")
                                .withType(COMPLEX)
                                .withComplexField(newCaseField()
                                    .withId("noteID")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId("category")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .withComplexField(newCaseField()
                                    .withId("tags")
                                    .withFieldType(aFieldType()
                                        .withId("Text")
                                        .withType("Text")
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())

                .withComplexField(newCaseField()
                    .withId("location")
                    .withFieldType(aFieldType()
                        .withId("AddressComplex")
                        .withType(COMPLEX)
                        .withComplexField(newCaseField()
                            .withId("AddressLine1")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("AddressLine2")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("City")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("State")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("Country")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("PostalCode")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(note).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return note;
    }

    private CaseFieldDefinition noteWithNestedFieldsWithDeletePermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setDelete(true);

        CaseFieldDefinition note = noteWithNestedFieldsWithoutDeletePermission();
        note.setAccessControlLists(List.of(deletePermission));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(note).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return note;
    }

    private CaseFieldDefinition caseCategoryFieldWithoutDeletePermission() {
        CaseFieldDefinition caseCategory = newCaseField()
            .withId("caseCategory")
            .withFieldType(aFieldType()
                .withId("CaseCategoryComplex")
                .withType(COMPLEX)

                .withComplexField(newCaseField()
                    .withId("value")
                    .withFieldType(aFieldType()
                        .withId("ValueComplex")
                        .withType(COMPLEX)
                        .withComplexField(newCaseField()
                            .withId("code")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .withComplexField(newCaseField()
                            .withId("label")
                            .withFieldType(aFieldType()
                                .withId("Text")
                                .withType("Text")
                                .build())
                            .build())
                        .build())
                    .build())

                .withComplexField(newCaseField()
                    .withId("list_items")
                    .withFieldType(aFieldType()
                        .withId("ListItemsCollection")
                        .withType(COLLECTION)
                        .withCollectionFieldType(aFieldType()
                            .withId("ListItemComplex")
                            .withType(COMPLEX)

                            .withComplexField(newCaseField()
                                .withId("id")
                                .withFieldType(aFieldType()
                                    .withId("Text")
                                    .withType("Text")
                                    .build())
                                .build())

                            .withComplexField(newCaseField()
                                .withId("value")
                                .withFieldType(aFieldType()
                                    .withId("ValueComplex")
                                    .withType(COMPLEX)
                                    .withComplexField(newCaseField()
                                        .withId("code")
                                        .withFieldType(aFieldType()
                                            .withId("Text")
                                            .withType("Text")
                                            .build())
                                        .build())
                                    .withComplexField(newCaseField()
                                        .withId("label")
                                        .withFieldType(aFieldType()
                                            .withId("Text")
                                            .withType("Text")
                                            .build())
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(caseCategory).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return caseCategory;
    }

    private CaseFieldDefinition caseCategoryFieldWithDeletePermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setDelete(true);

        CaseFieldDefinition caseCategory = caseCategoryFieldWithoutDeletePermission();
        caseCategory.setAccessControlLists(List.of(deletePermission));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(caseCategory).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return caseCategory;
    }

    @Test
    void shouldCaptureDeletionOfNullFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String nestedComplexTypeArrayPayload = """
            {
               "caseCategory": {
                   "value": {
                       "code": null,
                       "label": "Test"
                   },
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        final String newDataString = """
            {
               "caseCategory": {
                    "value": {
                       "label": "Test"
                   },
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
                }
            }
            """;

        String expectedMessage = "A child 'code' of 'caseCategory' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldDoNothingWhenNewNullFieldAddedToComplexType() {
        final String nestedComplexTypeArrayPayload = """
            {
               "caseCategory": {
                   "value": {
                       "label": null
                   },
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": null,
                               "label": null
                           }
                       }
                   ]
               }
            }
            """;

        final String newDataString = """
            {
               "caseCategory": {
                    "value": {
                       "code": null,
                       "label": "Test"
                   },
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
                }
            }
            """;

        JsonNode existingData = getJsonNode(nestedComplexTypeArrayPayload);
        JsonNode newData = getJsonNode(newDataString);
        boolean canDelete = service.canDeleteCaseFields(newData, existingData,
            caseCategoryFieldWithoutDeletePermission(), ACCESS_PROFILES);

        assertTrue(canDelete);
    }

    @Test
    void shouldNotPermitDeletionOfValueFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
                }
            }
            """;

        String expectedMessage = "A child 'value' of 'caseCategory' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfValueFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": null,
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        String expectedMessage = "A child 'value' of 'caseCategory' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfArrayFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": {
                       "code": "Test",
                       "label": "Test"
                   }
               }
            }
            """;

        String expectedMessage = "A child 'list_items' of 'caseCategory' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfArrayNodeFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": {
                       "code": "Test",
                       "label": "Test"
                   },
                   "list_items": [
                       {
                           "id": "444",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        String expectedMessage = "A child 'list_items' of 'caseCategory' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfValueFieldFromNestedComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        String expectedMessage = "Field 'value' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfValueFieldFromNestedComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": null,
                   "list_items": [
                       {
                           "id": "123456",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        String expectedMessage = "Field 'value' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfArrayFieldFromNestedComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": {
                       "code": "Test",
                       "label": "Test"
                   }
               }
            }
            """;

        String expectedMessage = "Field 'list_items' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfArrayNodeFieldFromNestedComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
               "caseCategory": {
                   "value": {
                       "code": "Test",
                       "label": "Test"
                   },
                   "list_items": [
                       {
                           "id": "444",
                           "value": {
                               "code": "Test",
                               "label": "Test"
                           }
                       }
                   ]
               }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123456\"' under '.list_items'.";

        assertTrue(assertDeleteAccessAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Category' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": null
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Category' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfValueFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfValueFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": null,
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfIdFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    }
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfSecondIdFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    }
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfSecondFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Category": "Work"
                    },
                  "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tag' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfNullStringIdFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "null"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfNullIdFieldFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": null
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfNullArrayNodeFromComplexTypeArrayWithoutPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": []
              }
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfFieldFromComplexTypeArrayWithPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Field 'Category' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": null
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Field 'Category' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfValueFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Missing nodes. ExistingData: {\"Tag\":\"private\",\"Category\":\"Personal\"}, "
            + "NewData: null";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfValueFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                    "value": null,
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Missing nodes. ExistingData: {\"Tag\":\"private\",\"Category\":\"Personal\"}, "
            + "NewData: null";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfIdFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    }
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfSecondIdFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    }
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"456\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfSecondFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Category": "Work"
                    },
                  "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Field 'Tag' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfNullStringIdFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "null"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfNullIdFieldFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": [
                  {
                     "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": null
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfNullArrayNodeFromComplexTypeArrayWithDeletePermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": []
              }
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(complexTypeArrayPayload, newDataString, noteWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfIdFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    }
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfSecondIdFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    }
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitChangeOfIdFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "999"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfValueFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfValueFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": null,
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tags' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfFieldOfValueFieldFromArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "A child 'Tag' of 'Tags' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithoutDeletePermission(),
            Level.INFO, expectedMessage));
    }

    @Test
    void shouldReturnIfNoChangeInArrayNodeWithoutPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(arrayPayload);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, tagsWithoutDeletePermission(), ACCESS_PROFILES));
        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldPermitDeletionOfIdFieldFromArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    }
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfSecondIdFieldFromArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    }
                  }
                ]
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"456\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitChangeOfIdFieldFromArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "999"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "Deleted collection item with ID '\"123\"' under '.Tags'.";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfValueFieldFromArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "Missing nodes. ExistingData: {\"Tag\":\"private\",\"Category\":\"Personal\"}, "
            + "NewData: null";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfValueFieldFromArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": null,
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "Missing nodes. ExistingData: {\"Tag\":\"private\",\"Category\":\"Personal\"}, "
            + "NewData: null";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfFieldOfValueFieldFromArrayNodeWithDeletedPermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        String expectedMessage = "Field 'Tag' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(arrayPayload, newDataString, tagsWithDeletePermission(),
            Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldReturnIfNoChangeInArrayNodeWithDeletePermission() {
        final String newDataString = """
             {
                "Tags": [
                  {
                    "value": {
                      "Tag": "private",
                      "Category": "Personal"
                    },
                    "id": "123"
                  },
                  {
                    "value": {
                      "Tag": "public",
                      "Category": "Work"
                    },
                    "id": "456"
                  }
                ]
            }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(arrayPayload);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, tagsWithDeletePermission(), ACCESS_PROFILES));
        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldPermitDeletionOfArrayNodeOfDocumentsCollectionWithoutDeletePermission() {
        CaseFieldDefinition caseType = newCaseField()
            .withId("Documents")
            .withFieldType(aFieldType()
                .withType(COLLECTION)
                .withCollectionFieldType(aFieldType()
                    .withType(DOCUMENT)
                    .withId(DOCUMENT)
                    .build())
                .build())
            .withOrder(1)
            .withAcl(anAcl()
                .withRole("caseworker-probate-loa1")
                .withCreate(false)
                .withUpdate(false)
                .withDelete(false)
                .withRead(true)
                .build())
            .build();
        JsonNode newData = getJsonNode("""
            {
              "Documents": [
                {
                  "id": "CollectionField2",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                }
              ]
            }""");
        JsonNode existingData = getJsonNode("""
            {
              "Documents": [
                {
                  "id": "CollectionField1",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                },
                {
                  "id": "CollectionField2",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                }
              ]
            }""");

        setupLogging().setLevel(Level.INFO);
        assertFalse(service.canDeleteCaseFields(newData, existingData, caseType, ACCESS_PROFILES));

        String expectedMessage = "A child 'Documents' of 'Documents' has been deleted but has no Delete ACL";

        loggingEventList = listAppender.list;
        assertAll(
            () -> assertTrue(loggingEventList.stream().allMatch(log -> log.getLevel() == Level.INFO)),
            () -> assertTrue(loggingEventList.stream().anyMatch(log ->
                log.getFormattedMessage().equals(expectedMessage)))
        );
    }

    @Test
    void shouldPermitDeletionOfBasicFieldOfDocumentsCollectionWithoutDeletePermission() {
        CaseFieldDefinition caseType = newCaseField()
            .withId("Documents")
            .withFieldType(aFieldType()
                .withType(COLLECTION)
                .withCollectionFieldType(aFieldType()
                    .withType(DOCUMENT)
                    .withId(DOCUMENT)
                    .build())
                .build())
            .withOrder(1)
            .withAcl(anAcl()
                .withRole("caseworker-probate-loa1")
                .withCreate(false)
                .withUpdate(false)
                .withDelete(false)
                .withRead(true)
                .build())
            .build();
        JsonNode newData = getJsonNode("""
            {
              "Documents": [
                {
                  "id": "CollectionField1",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                },
                {
                  "id": "CollectionField2",
                  "value": {
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                }
              ]
            }""");
        JsonNode existingData = getJsonNode("""
            {
              "Documents": [
                {
                  "id": "CollectionField1",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                },
                {
                  "id": "CollectionField2",
                  "value": {
                    "document_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943",
                    "document_binary_url": "{{DM_STORE_BASE_URL}}/documents/\
            ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                    "document_filename": "Elastic Search test Case.png --> updated by Solicitor 1"
                  }
                }
              ]
            }""");

        setupLogging().setLevel(Level.DEBUG);
        assertFalse(service.canDeleteCaseFields(newData, existingData, caseType, ACCESS_PROFILES));

        String expectedMessage = "Returning current field 'Documents', as target field document_url is a type which "
            + "does not contain sub-fields";

        loggingEventList = listAppender.list;
        assertAll(
            () -> assertTrue(loggingEventList.stream().anyMatch(log ->
                log.getLevel() == Level.DEBUG && log.getFormattedMessage().equals(expectedMessage))
            )
        );
    }

    @Test
    void shouldNotPermitDeletionOfAddressLine2FieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine1": "123 Main Street",
                   "AddressLine2": null,
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'AddressLine2' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfLocationFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 }
               }
             }
            """;

        String expectedMessage = "A child 'location' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfAddressLine2FieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'AddressLine1' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfNoteIDFieldInInnerComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'noteID' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfTagsFieldInInnerComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": null
                   }
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'tags' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfAdditionalInfoFieldInInnerComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": null
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'additionalInfo' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfAdditionalInfoFieldInInnerComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables."
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "A child 'additionalInfo' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfContentFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "type": "PersonalNote",
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "A child 'content' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitNullOfContentFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "type": "PersonalNote",
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "content": null,
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "A child 'content' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldNotPermitDeletionOfTypeFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "content": {
                      "title": "Meeting Notes",
                      "body": "Discussion about project timelines and deliverables.",
                      "additionalInfo": {
                        "noteID": "abc123",
                        "category": "Meeting",
                        "tags": "project, timeline, deliverable"
                      }
                    },
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "A child 'type' of 'Note' has been deleted but has no Delete ACL";

        assertFalse(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutDeletePermission(), Level.INFO, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfAddressLine2FieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine1": "123 Main Street",
                   "AddressLine2": null,
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'AddressLine2' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfLocationFieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 }
               }
             }
            """;

        String expectedMessage = "Field 'location' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfAddressLine2FieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'AddressLine1' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfNoteIDFieldInInnerComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "category": "Meeting",
                     "tags": "project, timeline, deliverable"
                   }
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'noteID' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfTagsFieldInInnerComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": {
                     "noteID": "abc123",
                     "category": "Meeting",
                     "tags": null
                   }
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'tags' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfAdditionalInfoFieldInInnerComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables.",
                   "additionalInfo": null
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'additionalInfo' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfAdditionalInfoFieldInInnerComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
             {
               "Note": {
                 "type": "PersonalNote",
                 "metadata": {
                   "authorName": "John Doe",
                   "creationDate": "2024-11-04"
                 },
                 "content": {
                   "title": "Meeting Notes",
                   "body": "Discussion about project timelines and deliverables."
                 },
                 "location": {
                   "AddressLine1": "address1",
                   "AddressLine2": "Suite 500",
                   "City": "Anytown",
                   "State": "Anystate",
                   "Country": "AnyCountry",
                   "PostalCode": "12345"
                 }
               }
             }
            """;

        String expectedMessage = "Field 'additionalInfo' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfContentFieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "type": "PersonalNote",
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "Field 'content' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitNullOfContentFieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "type": "PersonalNote",
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "content": null,
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "Field 'content' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfTypeFieldInComplexNestedObjectWithDeletePermission() {
        final String newDataString = """
              {
                  "Note": {
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    },
                    "content": {
                      "title": "Meeting Notes",
                      "body": "Discussion about project timelines and deliverables.",
                      "additionalInfo": {
                        "noteID": "abc123",
                        "category": "Meeting",
                        "tags": "project, timeline, deliverable"
                      }
                    },
                    "location": {
                      "AddressLine1": "123 Main Street",
                      "AddressLine2": "Suite 500",
                      "City": "Anytown",
                      "State": "Anystate",
                      "Country": "AnyCountry",
                      "PostalCode": "12345"
                    }
                  }
                }
            """;

        String expectedMessage = "Field 'type' is missing or null in new data but exists in existing data.";

        assertTrue(assertDeleteAccessAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldDoNothingNullOfRootNoteFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
              {
                "Note": null
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(complexTypePayload);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingIfExistingDataIsEmpty() {
        final String existingDataString = """
              {
              }
            """;

        final String newDataString = """
              {
                "Note": {
                    "type": "test"
                }
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(existingDataString);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingIfExistingDataRootNoteFieldIsEmpty() {
        final String existingDataString = """
              {
                "Note": {}
              }
            """;

        final String newDataString = """
              {
                "Note": {
                    "type": "test"
                }
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(existingDataString);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingIfNewDataIsEmpty() {
        final String existingDataString = """
              {
                "Note": {
                    "type": "test"
                }
              }
            """;

        final String newDataString = """
              {
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(existingDataString);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingIfNewDataRootNoteFieldIsEmpty() {
        final String existingDataString = """
              {
                "Note": {
                    "type": "test"
                }
              }
            """;

        final String newDataString = """
              {
                "Note": {}
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(existingDataString);
        JsonNode newData = getJsonNode(newDataString);

        assertTrue(service.canDeleteCaseFields(newData, existingData, noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldThrowExceptionIfFieldIsNotFoundInCaseField() {
        final String existingDataString = """
              {
                "Note": {
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04",
                      "missingField": "test data"
                    }
                }
              }
            """;

        final String newDataString = """
              {
                "Note": {
                    "metadata": {
                      "authorName": "John Doe",
                      "creationDate": "2024-11-04"
                    }
                }
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        JsonNode existingData = getJsonNode(existingDataString);
        JsonNode newData = getJsonNode(newDataString);

        assertThrows(ResourceNotFoundException.class, () -> service.canDeleteCaseFields(newData, existingData,
            noteWithNestedFieldsWithoutDeletePermission(),
            ACCESS_PROFILES));

        String expectedMessage = "Field 'missingField' is missing or null in new data but exists in existing data.";
        assertEquals(expectedMessage, listAppender.list.getFirst().getFormattedMessage());
    }

    private CaseFieldDefinition generatedCaseDocumentsField() {
        CaseFieldDefinition generatedCaseDocuments = newCaseField()
            .withId("generatedCaseDocuments")
            .withFieldType(aFieldType()
                .withId("DocumentCollection")
                .withType(COLLECTION)
                .withCollectionFieldType(aFieldType()
                    .withId("DocumentComplex")
                    .withType(COMPLEX)
                    .withComplexField(newCaseField()
                        .withId("createdBy")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())

                    .withComplexField(newCaseField()
                        .withId("documentLink")
                        .withFieldType(aFieldType()
                            .withId("DocumentLinkComplex")
                            .withType(COMPLEX)
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("documentName")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("documentSize")
                        .withFieldType(aFieldType()
                            .withId("Number")
                            .withType("Number")
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("documentType")
                        .withFieldType(aFieldType()
                            .withId("Text")
                            .withType("Text")
                            .build())
                        .build())
                    .withComplexField(newCaseField()
                        .withId("createdDatetime")
                        .withFieldType(aFieldType()
                            .withId("DateTime")
                            .withType("DateTime")
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(generatedCaseDocuments).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return generatedCaseDocuments;
    }

    private CaseFieldDefinition generatedCaseDocumentsFieldWithDeletePermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setDelete(true);

        CaseFieldDefinition document = generatedCaseDocumentsField();
        document.setAccessControlLists(List.of(deletePermission));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(document).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return document;
    }

    @Test
    void shouldNotPermitDeletionOfCategoryIDdFieldFromDocumentObjectWithoutDeletePermission() {
        final String existingDataString = """
              {
                  "generatedCaseDocuments": [
                       {
                           "id": "123",
                           "value": {
                               "createdBy": "Test",
                               "documentLink": {
                                   "category_id": "detailsOfClaim",
                                   "document_url": "http_document_url",
                                   "document_filename": "document.pdf",
                                   "document_binary_url": "http://document/binary"
                               },
                               "documentName": "document.pdf",
                               "documentSize": 34805,
                               "documentType": "CLAIM",
                               "createdDatetime": "2024-10-16T10:47:03"
                           }
                       }
                   ]
                }
            """;

        final String newDataString = """
              {
                  "generatedCaseDocuments": [
                       {
                           "id": "123",
                           "value": {
                               "createdBy": "Test",
                               "documentLink": {
                                   "document_url": "http_document_url",
                                   "document_filename": "document.pdf",
                                   "document_binary_url": "http://document/binary"
                               },
                               "documentName": "document.pdf",
                               "documentSize": 34805,
                               "documentType": "CLAIM",
                               "createdDatetime": "2024-10-16T10:47:03"
                           }
                       }
                   ]
                }
            """;

        String expectedMessage = "Returning current field 'documentLink', as target field category_id is a type which"
            + " does not contain sub-fields";

        assertFalse(assertDeleteAccessAndLogging(existingDataString, newDataString,
            generatedCaseDocumentsField(), Level.DEBUG, expectedMessage));
    }

    @Test
    void shouldPermitDeletionOfCategoryIDdFieldFromDocumentObjectWithDeletePermission() {
        final String existingDataString = """
              {
                  "generatedCaseDocuments": [
                       {
                           "id": "123",
                           "value": {
                               "createdBy": "Test",
                               "documentLink": {
                                   "category_id": "detailsOfClaim",
                                   "document_url": "http_document_url",
                                   "document_filename": "document.pdf",
                                   "document_binary_url": "http://document/binary"
                               },
                               "documentName": "document.pdf",
                               "documentSize": 34805,
                               "documentType": "CLAIM",
                               "createdDatetime": "2024-10-16T10:47:03"
                           }
                       }
                   ]
                }
            """;

        final String newDataString = """
              {
                  "generatedCaseDocuments": [
                       {
                           "id": "123",
                           "value": {
                               "createdBy": "Test",
                               "documentLink": {
                                   "document_url": "http_document_url",
                                   "document_filename": "document.pdf",
                                   "document_binary_url": "http://document/binary"
                               },
                               "documentName": "document.pdf",
                               "documentSize": 34805,
                               "documentType": "CLAIM",
                               "createdDatetime": "2024-10-16T10:47:03"
                           }
                       }
                   ]
                }
            """;


        String expectedMessage = "Returning current field 'documentLink', as target field category_id is a type which"
            + " does not contain sub-fields";

        assertTrue(assertDeleteAccessAndLogging(existingDataString, newDataString,
            generatedCaseDocumentsFieldWithDeletePermission(), Level.DEBUG, expectedMessage));
    }

    private Logger setupLogging() {
        listAppender = new ListAppender<>();
        listAppender.start();
        logger = (Logger) LoggerFactory.getLogger(DeleteAccessControlService.class);
        logger.detachAndStopAllAppenders();
        if (loggingEventList != null && !loggingEventList.isEmpty()) {
            loggingEventList.clear();
        }
        logger.addAppender(listAppender);
        return logger;
    }

    private JsonNode getJsonNode(String content) {
        ObjectMapperService mapper = new DefaultObjectMapperService(new ObjectMapper());
        return mapper.convertStringToObject(content, JsonNode.class);
    }

    private boolean assertDeleteAccessAndLogging(String existingDataJson, String newDataJson, CaseFieldDefinition field,
                                                 Level expectedLogLevel, String expectedLogMessage) {
        setupLogging().setLevel(expectedLogLevel);

        JsonNode existingData = getJsonNode(existingDataJson);
        JsonNode newData = getJsonNode(newDataJson);

        boolean canDelete = service.canDeleteCaseFields(newData, existingData, field, ACCESS_PROFILES);

        loggingEventList = listAppender.list;

        assertAll(
            () -> assertTrue(loggingEventList.stream().anyMatch(log -> log.getLevel() == expectedLogLevel
                    && log.getFormattedMessage().equals(expectedLogMessage)),
                "Expected log message not found: " + expectedLogMessage)
        );

        return canDelete;
    }

    @AfterEach
    void tearDown() {
        if (listAppender != null) {
            listAppender.stop();
        }
        if (logger != null) {
            logger.detachAndStopAllAppenders();
        }
    }
}
