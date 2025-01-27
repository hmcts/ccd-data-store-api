package uk.gov.hmcts.ccd.domain.service.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DOCUMENT;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ACCESS_PROFILES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getTagFieldDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ConditionalFieldRestorerTest {

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

    static String nestedCaseCategoryComplexTypeArrayPayload = """
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


    @Mock
    private CaseAccessService caseAccessService;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    private List<ILoggingEvent> loggingEventList;
    private ConditionalFieldRestorer service;
    private AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);

        service = new ConditionalFieldRestorer(caseAccessService);
        when(caseAccessService.getAccessProfilesByCaseReference(anyString())).thenReturn(ACCESS_PROFILES);
    }

    private CaseFieldDefinition noteWithoutCreateAndReadPermission() {
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

    private CaseFieldDefinition noteWithCreatePermissionWithoutReadPermission() {
        AccessControlList deletePermission = new AccessControlList();
        deletePermission.setAccessProfile("caseworker-probate-loa1");
        deletePermission.setCreate(true);
        deletePermission.setRead(false);

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

    private CaseFieldDefinition tagWithoutCreateAndReadPermission() {
        CaseFieldDefinition tags = getTagFieldDefinition();

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(tags).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return tags;
    }

    private CaseFieldDefinition tagsWithCreatePermissionWithoutReadPermission() {
        AccessControlList controlList = new AccessControlList();
        controlList.setAccessProfile("caseworker-probate-loa1");
        controlList.setCreate(true);
        controlList.setRead(false);

        CaseFieldDefinition tags = getTagFieldDefinition();
        tags.setAccessControlLists(List.of(controlList));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(tags).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return tags;
    }

    private CaseFieldDefinition noteWithNestedFieldsWithoutCreateAndReadPermission() {
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

    private CaseFieldDefinition noteWithNestedFieldsWithCreateAndWithoutReadPermission() {
        AccessControlList controlList = new AccessControlList();
        controlList.setAccessProfile("caseworker-probate-loa1");
        controlList.setCreate(true);
        controlList.setRead(false);

        CaseFieldDefinition note = noteWithNestedFieldsWithoutCreateAndReadPermission();
        note.setAccessControlLists(List.of(controlList));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(note).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return note;
    }

    private CaseFieldDefinition caseCategoryFieldWithoutCreateAndReadPermission() {
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

    private CaseFieldDefinition caseCategoryFieldWithCreateWithoutReadPermission() {
        AccessControlList accessControlList = new AccessControlList();
        accessControlList.setAccessProfile("caseworker-probate-loa1");
        accessControlList.setCreate(true);
        accessControlList.setRead(false);

        CaseFieldDefinition caseCategory = caseCategoryFieldWithoutCreateAndReadPermission();
        caseCategory.setAccessControlLists(List.of(accessControlList));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(caseCategory).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return caseCategory;
    }

    private CaseFieldDefinition generatedCaseDocumentsFieldWithoutCreateAndReadPermission() {
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

    private CaseFieldDefinition generatedCaseDocumentsFieldWithCreateWithoutReadPermission() {
        AccessControlList accessControlList = new AccessControlList();
        accessControlList.setAccessProfile("caseworker-probate-loa1");
        accessControlList.setCreate(true);
        accessControlList.setRead(false);

        CaseFieldDefinition document = generatedCaseDocumentsFieldWithoutCreateAndReadPermission();
        document.setAccessControlLists(List.of(accessControlList));

        final CaseTypeDefinition caseTypeDefinition = newCaseType().withField(document).build();
        caseTypeDefinition.getCaseFieldDefinitions().forEach(CaseFieldDefinition::propagateACLsToNestedFields);

        return document;
    }

    @Test
    void shouldAddMissingNullFieldToComplexTypeWhenCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'code' under 'value'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedComplexTypeArrayPayload, newDataString,
            caseCategoryFieldWithCreateWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("label").asText()),
            () -> assertEquals("null", result.get("caseCategory").get("value").get("code").asText()),
            () -> assertEquals("Test",
                result.get("caseCategory").get("list_items").get(0).get("value").get("label").asText()),
            () -> assertEquals("Test",
                result.get("caseCategory").get("list_items").get(0).get("value").get("code").asText())
        );
    }

    @Test
    void shouldKeepWhenNewNullFieldAddedToComplexTypeWhenCreateWithoutReadPermission() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(caseCategoryFieldWithCreateWithoutReadPermission()).build();
        Map<String, JsonNode> existingData = getJsonMapNode(nestedComplexTypeArrayPayload);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);
        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition, newData,
            existingData, "123");

        assertAll(
            () -> assertEquals("Test", filteredFields.get("caseCategory").get("value").get("label").asText()),
            () -> assertEquals("null", filteredFields.get("caseCategory").get("value").get("code").asText()),
            () -> assertEquals("Test",
                filteredFields.get("caseCategory").get("list_items").get(0).get("value").get("label").asText()),
            () -> assertEquals("Test",
                filteredFields.get("caseCategory").get("list_items").get(0).get("value").get("code").asText())
        );
    }

    @Test
    void shouldAddMissingValueComplexFieldToComplexTypeWhenCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'value' under 'caseCategory'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithCreateWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertTrue(result.get("caseCategory").get("value").has("code")),
            () -> assertTrue(result.get("caseCategory").get("value").has("label")),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("code").asText()),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("label").asText())
        );
    }

    @Test
    void shouldAddMissingValueSubFieldsToComplexTypeWhenCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'code' under 'value'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithCreateWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertTrue(result.get("caseCategory").get("value").has("code")),
            () -> assertTrue(result.get("caseCategory").get("value").has("label")),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("code").asText()),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("label").asText())
        );
    }

    @Test
    void shouldAddMissingListItemsArrayToComplexTypeWhenCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'list_items' under 'caseCategory'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithCreateWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertTrue(result.get("caseCategory").has("list_items")),
            () -> assertEquals(1, result.get("caseCategory").get("list_items").size())
        );
    }

    @Test
    void shouldAddMissingListItemsNodeToComplexTypeWhenCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"123456\"' under 'list_items'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithCreateWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertTrue(result.get("caseCategory").has("list_items")),
            () -> assertEquals(2, result.get("caseCategory").get("list_items").size())
        );
    }

    @Test
    void shouldDoNothingForMissingValueComplexFieldToComplexTypeWhenWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'value' under 'caseCategory'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("list_items")),
            () -> assertFalse(result.get("caseCategory").has("value"))
        );
    }

    @Test
    void shouldDoNothingForNullValueWhenWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'label' under 'value'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("list_items")),
            () -> assertEquals(1, result.get("caseCategory").get("list_items").size()),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertTrue(result.get("caseCategory").get("value").asText().isEmpty())
        );
    }

    @Test
    void shouldDoNothingForMissingListItemArrayWhenWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'list_items' under 'caseCategory'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("label").asText()),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("code").asText()),
            () -> assertFalse(result.get("caseCategory").has("list_items"))
        );
    }

    @Test
    void shouldDoNothingForMissingListItemNodeWhenWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123456\"' under 'list_items'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(nestedCaseCategoryComplexTypeArrayPayload,
            newDataString,
            caseCategoryFieldWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("caseCategory")),
            () -> assertTrue(result.get("caseCategory").has("value")),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("label").asText()),
            () -> assertEquals("Test", result.get("caseCategory").get("value").get("code").asText()),
            () -> assertTrue(result.get("caseCategory").has("list_items")),
            () -> assertEquals(1, result.get("caseCategory").get("list_items").size()),
            () -> assertEquals("444", result.get("caseCategory").get("list_items").get(0).get("id").asText())
        );
    }

    @Test
    void shouldAddMissingCategoryFieldToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'Category' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertNotNull(result.get("Note").get("Tags").get(0).get("value").get("Category")),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertEquals("public", result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText()),
            () -> assertEquals("public", result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Note").get("Tags").get(1).get("value").get("Category").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCategoryFieldSetToNullWhenWithCreateWithoutReadPermission() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithCreatePermissionWithoutReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(complexTypeArrayPayload);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertNotNull(result.get("Note").get("Tags").get(0).get("value").get("Category")),
            () -> assertEquals("null",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText()),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText())
        );
    }

    @Test
    void shouldAddMissingValueSubFieldsToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123",
                result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldAddNullValueToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'Category' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldAddFirstMissingNodeToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(3, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Note").get("Tags").get(0).has("id")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("private", result.get("Note").get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Note").get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddSecondMissingNodeToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"456\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(3, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Note").get("Tags").get(1).has("id")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddMissingSubFirstFieldToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText())
        );
    }

    @Test
    void shouldAddFirstNullIDToCollectionTypeWhenWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(3, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("null", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddSecondNullIDToCollectionTypeWhenWithCreateWithoutReadPermission() {
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
                    },
                    "id": null
                  }
                ]
              }
            }
            """;

        String expectedMessage = "Adding missing collection item with ID '\"456\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(3, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("null", result.get("Note").get("Tags").get(1).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddMissingNodesToCollectionTypeWhenWithCreateWithoutReadPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": []
              }
            }
            """;

        String expectedMessage = "Adding missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCategoryFieldMissingWhenWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Category' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertFalse(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenValueMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").isEmpty()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenValueIsNullWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").isEmpty()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenNodeIDMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Note").get("Tags").get(0).has("id")),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenSecondNodeIDMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"456\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Note").get("Tags").get(1).has("id"))
        );
    }

    @Test
    void shouldDoNothingWhenSecondSubFieldMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertFalse(result.get("Note").get("Tags").get(1).get("value").has("Tag")),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenNodeIDNullStringWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("id")),
            () -> assertEquals("null", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenNodeIDNullWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertEquals(2, result.get("Note").get("Tags").size()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Note").get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private",
                result.get("Note").get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal",
                result.get("Note").get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(0).has("id")),
            () -> assertEquals("null", result.get("Note").get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Note").get("Tags").get(1).has("value")),
            () -> assertEquals("public",
                result.get("Note").get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work",
                result.get("Note").get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Note").get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenNodesAreMissingWithoutCreateWithoutReadPermission() {
        final String newDataString = """
            {
              "Note": {
                "Tags": []
              }
            }
            """;

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypeArrayPayload, newDataString,
            noteWithoutCreateAndReadPermission(),
            Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Note")),
            () -> assertTrue(result.get("Note").has("Tags")),
            () -> assertTrue(result.get("Note").get("Tags").isEmpty())
        );
    }

    @Test
    void shouldAddMissingCollectionTypeNoeIDWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagsWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(3, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Tags").get(0).has("id")),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddMissingSecondCollectionTypeNodeIDWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"456\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagsWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(3, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Tags").get(1).has("id")),
            () -> assertTrue(result.get("Tags").get(2).has("value")),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(2).get("id").asText())
        );
    }

    @Test
    void shouldAddChangedCollectionTypeNodeIDWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagsWithCreatePermissionWithoutReadPermission(),
            Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(3, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("999", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(2).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(2).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(2).get("value").get("Category").asText()),
            () -> assertEquals("123", result.get("Tags").get(2).get("id").asText())
        );
    }

    private static Stream<TestCase> tagsTestCases() {
        return Stream.of(
            new TestCase(
                """
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
                """,
                "Adding missing field 'Tag' under 'Tags'."
            ),
            new TestCase(
                """
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
                """,
                "Adding missing field 'Tag' under 'Tags'."
            ),
            new TestCase(
                """
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
                """,
                "Adding missing field 'Tag' under 'Tags'."
            )
        );
    }

    @ParameterizedTest
    @MethodSource("tagsTestCases")
    void shouldHandleTagFilteringWithCreatePermissionWithoutReadPermission(TestCase testCase) {
        Map<String, JsonNode> result = assertFilterServiceAndLogging(
            arrayPayload,
            testCase.inputJson,
            tagsWithCreatePermissionWithoutReadPermission(),
            Level.INFO,
            testCase.expectedMessage
        );

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    private record TestCase(String inputJson, String expectedMessage) {
    }

    @Test
    void shouldReturnIfNoChangeInArrayNodeWithoutCreateAndReadPermission() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(tagWithoutCreateAndReadPermission()).build();
        Map<String, JsonNode> existingData = getJsonMapNode(arrayPayload);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition, newData,
            existingData, "123");
        assertEquals(existingData, filteredFields);

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingWhenSecondCollectionTypeNodeIDMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"456\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Tags").get(1).has("id"))
        );
    }

    @Test
    void shouldDoNothingWhenCollectionTypeNodeIDMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertFalse(result.get("Tags").get(0).has("id")),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCollectionTypeNodeIDChangedWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing collection item with ID '\"123\"' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("999", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCollectionTypeValueMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).get("value").isEmpty()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCollectionTypeValueIsNullWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).get("value").isEmpty()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("public", result.get("Tags").get(1).get("value").get("Tag").asText()),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldDoNothingWhenCollectionTypeSubFieldMissingWithoutCreateWithoutReadPermission() {
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

        String expectedMessage = "Missing field 'Tag' under 'Tags'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(arrayPayload, newDataString,
            tagWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.containsKey("Tags")),
            () -> assertEquals(2, result.get("Tags").size()),
            () -> assertTrue(result.get("Tags").get(0).has("value")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(0).get("value").has("Category")),
            () -> assertEquals("private", result.get("Tags").get(0).get("value").get("Tag").asText()),
            () -> assertEquals("Personal", result.get("Tags").get(0).get("value").get("Category").asText()),
            () -> assertTrue(result.get("Tags").get(0).has("id")),
            () -> assertEquals("123", result.get("Tags").get(0).get("id").asText()),
            () -> assertTrue(result.get("Tags").get(1).has("value")),
            () -> assertFalse(result.get("Tags").get(1).get("value").has("Tag")),
            () -> assertTrue(result.get("Tags").get(1).get("value").has("Category")),
            () -> assertEquals("Work", result.get("Tags").get(1).get("value").get("Category").asText()),
            () -> assertEquals("456", result.get("Tags").get(1).get("id").asText())
        );
    }

    @Test
    void shouldReturnIfNoChangeInArrayNodeWithCreatePermissionWithoutReadPermission() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(tagsWithCreatePermissionWithoutReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(arrayPayload);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition, newData,
            existingData, "123");
        assertEquals(existingData, filteredFields);

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldAddMissingDocumentNodeToDocumentCollectionWithCreateWithoutRead() {
        CaseFieldDefinition caseFieldDefinition = newCaseField()
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
                .withCreate(true)
                .withUpdate(false)
                .withDelete(false)
                .withRead(false)
                .build())
            .build();
        Map<String, JsonNode> newData = getJsonMapNode("""
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
        Map<String, JsonNode> existingData = getJsonMapNode("""
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(caseFieldDefinition).build();

        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition, newData,
            existingData, "123");

        assertAll(
            () -> assertTrue(filteredFields.containsKey("Documents")),
            () -> assertTrue(filteredFields.get("Documents").isArray()),
            () -> assertEquals(2, filteredFields.get("Documents").size()),
            () -> assertEquals("CollectionField2", filteredFields.get("Documents").get(0).get("id").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).has("value")),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75dd3943",
                filteredFields.get("Documents").get(0).get("value").get("document_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_binary_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                filteredFields.get("Documents").get(0).get("value").get("document_binary_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_filename")),
            () -> assertEquals("Elastic Search test Case.png --> updated by Solicitor 1",
                filteredFields.get("Documents").get(0).get("value").get("document_filename").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).has("id")),
            () -> assertEquals("CollectionField1", filteredFields.get("Documents").get(1).get("id").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).has("value")),
            () -> assertTrue(filteredFields.get("Documents").get(1).get("value").has("document_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg",
                filteredFields.get("Documents").get(1).get("value").get("document_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).get("value").has("document_binary_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary",
                filteredFields.get("Documents").get(1).get("value").get("document_binary_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).get("value").has("document_filename")),
            () -> assertEquals("Elastic Search test Case.png --> updated by Solicitor 1",
                filteredFields.get("Documents").get(1).get("value").get("document_filename").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).has("id"))
        );

        String expectedMessage = "Adding missing collection item with ID '\"CollectionField1\"' under 'Documents'.";

        loggingEventList = listAppender.list;
        assertAll(
            () -> assertTrue(loggingEventList.stream().allMatch(log -> log.getLevel() == Level.INFO)),
            () -> assertTrue(loggingEventList.stream().anyMatch(log ->
                log.getFormattedMessage().equals(expectedMessage)))
        );
    }

    @Test
    void shouldDoNothingWhenMissingDocumentSubFieldWithoutCreateWithoutRead() {
        CaseFieldDefinition caseFieldDefinition = newCaseField()
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
                .withRead(false)
                .build())
            .build();
        Map<String, JsonNode> newData = getJsonMapNode("""
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
        Map<String, JsonNode> existingData = getJsonMapNode("""
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(caseFieldDefinition).build();

        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition, newData,
            existingData, "123");
        assertAll(
            () -> assertTrue(filteredFields.containsKey("Documents")),
            () -> assertTrue(filteredFields.get("Documents").isArray()),
            () -> assertEquals(2, filteredFields.get("Documents").size()),
            () -> assertEquals("CollectionField1", filteredFields.get("Documents").get(0).get("id").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).has("value")),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg",
                filteredFields.get("Documents").get(0).get("value").get("document_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_binary_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75yfhgfhg/binary",
                filteredFields.get("Documents").get(0).get("value").get("document_binary_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(0).get("value").has("document_filename")),
            () -> assertEquals("Elastic Search test Case.png --> updated by Solicitor 1",
                filteredFields.get("Documents").get(0).get("value").get("document_filename").asText()),
            () -> assertEquals("CollectionField2", filteredFields.get("Documents").get(1).get("id").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).has("value")),
            () -> assertFalse(filteredFields.get("Documents").get(1).get("value").has("document_url")),
            () -> assertTrue(filteredFields.get("Documents").get(1).get("value").has("document_binary_url")),
            () -> assertEquals("{{DM_STORE_BASE_URL}}/documents/ae5c9e4b-1385-483e-b1b7-607e75dd3943/binary",
                filteredFields.get("Documents").get(1).get("value").get("document_binary_url").asText()),
            () -> assertTrue(filteredFields.get("Documents").get(1).get("value").has("document_filename")),
            () -> assertEquals("Elastic Search test Case.png --> updated by Solicitor 1",
                filteredFields.get("Documents").get(1).get("value").get("document_filename").asText())
        );

        String expectedMessage = "Missing field 'document_url' under 'Documents'.";

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

        CaseTypeDefinition  caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();
        var newDataNode = getJsonMapNode(newDataString);
        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition,
            newDataNode, getJsonMapNode(complexTypePayload), "123");

        assertEquals(newDataNode, filteredFields);
    }

    @Test
    void shouldAddMissingLocationFieldInComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'location' under 'Note'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable", result.get("Note").get("content")
                    .get("additionalInfo").get("tags").asText()),
            () -> assertEquals("123 Main Street",
                result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText())
        );
    }

    @Test
    void shouldNotPermitNullOfAddressLine2FieldInComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'AddressLine1' under 'location'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText()),
            () -> assertEquals("123 Main Street",
                result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldAddMissingSubFieldInInnerComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'noteID' under 'additionalInfo'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("creationDate")),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("address1", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );

    }

    @Test
    void shouldDoNothingWhenSubFieldIsNullInInnerComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        CaseTypeDefinition  caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithCreateAndWithoutReadPermission()).build();
        Map<String, JsonNode> newDataNode = getJsonMapNode(newDataString);
        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition,
            newDataNode, getJsonMapNode(complexTypePayload), "123");

        assertEquals(newDataNode, filteredFields);
    }

    @Test
    void shouldAddMissingSubFieldsOfNullComplexFieldInInnerComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'noteID' under 'additionalInfo'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText()),
            () -> assertEquals("address1", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldAddMissingComplexFieldInInnerComplexNestedObjectWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'additionalInfo' under 'content'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText()),
            () -> assertEquals("address1", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldAddMissingParentComplexFieldWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'content' under 'Note'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertTrue(result.get("Note").get("location").has("AddressLine1")),
            () -> assertEquals("123 Main Street", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertTrue(result.get("Note").get("content").get("additionalInfo").has("noteID")),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertTrue(result.get("Note").get("content").get("additionalInfo").has("tags")),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText())
        );
    }

    @Test
    void shouldAddNullParentComplexFieldWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'additionalInfo' under 'content'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("123 Main Street",
                result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertTrue(result.get("Note").get("content").get("additionalInfo").has("noteID")),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText())
        );
    }

    @Test
    void shouldAddMissingTopSimpleFieldInParentNodeWithCreateAndWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'type' under 'Note'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithCreateAndWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.get("Note").has("type")),
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertTrue(result.get("Note").get("location").has("AddressLine1")),
            () -> assertEquals("123 Main Street",
                result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertEquals("abc123",
                result.get("Note").get("content").get("additionalInfo").get("noteID").asText()),
            () -> assertEquals("Meeting",
                result.get("Note").get("content").get("additionalInfo").get("category").asText()),
            () -> assertEquals("project, timeline, deliverable",
                result.get("Note").get("content").get("additionalInfo").get("tags").asText())
        );
    }

    @Test
    void shouldDoNothingWhenAddressLine2IsNullInComplexNestedObjectWithoutCreateAndReadPermission() {
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

        CaseTypeDefinition  caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();
        Map<String, JsonNode> newDataNode = getJsonMapNode(newDataString);
        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition,
            newDataNode, getJsonMapNode(complexTypePayload), "123");

        assertEquals(newDataNode, filteredFields);
    }

    private static Stream<TestCase> noteTestCases() {
        return Stream.of(
            new TestCase(
                """
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
                """,
                "Missing field 'location' under 'Note'."
            ),
            new TestCase(
                """
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
                """,
                "Missing field 'AddressLine1' under 'location'."
            ),
            new TestCase(
                """
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
                """,
                "Missing field 'noteID' under 'additionalInfo'."
            ),
            new TestCase(
                """
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
                """,
                "Missing field 'content' under 'Note'."
            ),
            new TestCase(
                """
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
                """,
                "Missing field 'type' under 'Note'."
            )
        );
    }

    @ParameterizedTest
    @MethodSource("noteTestCases")
    void shouldHandleMissingFieldsInComplexNestedObjects(TestCase testCase) {
        Map<String, JsonNode> result = assertFilterServiceAndLogging(
            complexTypePayload,
            testCase.inputJson,
            noteWithNestedFieldsWithoutCreateAndReadPermission(),
            Level.DEBUG,
            testCase.expectedMessage
        );

        assertEquals(getJsonMapNode(testCase.inputJson), result);
    }

    @Test
    void shouldDoNothingWhenSubFieldIsNullInInnerComplexNestedObjectWithoutCreateAndReadPermission() {
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

        CaseTypeDefinition  caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();
        Map<String, JsonNode> newDataNode = getJsonMapNode(newDataString);
        Map<String, JsonNode> filteredFields = service.restoreConditionalFields(caseTypeDefinition,
            newDataNode, getJsonMapNode(complexTypePayload), "123");

        assertEquals(newDataNode, filteredFields);
    }

    @Test
    void shouldDoNothingWhenComplexFieldIsNullInInnerComplexNestedObjectWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'category' under 'additionalInfo'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.get("Note").has("type")),
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertTrue(result.get("Note").has("content")),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertTrue(result.get("Note").get("content").get("additionalInfo").isObject()),
            () -> assertTrue(result.get("Note").has("location")),
            () -> assertEquals("address1", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldDoNothingWhenComplexFieldIsMissingInInnerComplexNestedObjectWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'additionalInfo' under 'content'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.get("Note").has("type")),
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertEquals("Meeting Notes", result.get("Note").get("content").get("title").asText()),
            () -> assertEquals("Discussion about project timelines and deliverables.",
                result.get("Note").get("content").get("body").asText()),
            () -> assertTrue(result.get("Note").has("location")),
            () -> assertEquals("address1", result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldDoNothingWhenParentComplexFieldIsNullInObjectWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'additionalInfo' under 'content'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(complexTypePayload, newDataString,
            noteWithNestedFieldsWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertAll(
            () -> assertTrue(result.get("Note").has("type")),
            () -> assertEquals("PersonalNote", result.get("Note").get("type").asText()),
            () -> assertTrue(result.get("Note").has("metadata")),
            () -> assertTrue(result.get("Note").get("metadata").has("authorName")),
            () -> assertEquals("John Doe", result.get("Note").get("metadata").get("authorName").asText()),
            () -> assertTrue(result.get("Note").get("metadata").has("creationDate")),
            () -> assertEquals("2024-11-04", result.get("Note").get("metadata").get("creationDate").asText()),
            () -> assertTrue(result.get("Note").has("content")),
            () -> assertTrue(result.get("Note").has("location")),
            () -> assertTrue(result.get("Note").get("location").has("AddressLine1")),
            () -> assertEquals("123 Main Street",
                result.get("Note").get("location").get("AddressLine1").asText()),
            () -> assertTrue(result.get("Note").get("location").has("AddressLine2")),
            () -> assertEquals("Suite 500", result.get("Note").get("location").get("AddressLine2").asText()),
            () -> assertTrue(result.get("Note").get("location").has("City")),
            () -> assertEquals("Anytown", result.get("Note").get("location").get("City").asText()),
            () -> assertTrue(result.get("Note").get("location").has("State")),
            () -> assertEquals("Anystate", result.get("Note").get("location").get("State").asText()),
            () -> assertTrue(result.get("Note").get("location").has("Country")),
            () -> assertEquals("AnyCountry", result.get("Note").get("location").get("Country").asText()),
            () -> assertTrue(result.get("Note").get("location").has("PostalCode")),
            () -> assertEquals("12345", result.get("Note").get("location").get("PostalCode").asText())
        );
    }

    @Test
    void shouldDoNothingNullOfRootNoteFieldInComplexNestedObjectWithoutDeletePermission() {
        final String newDataString = """
              {
                "Note": null
              }
            """;

        setupLogging().setLevel(Level.DEBUG);

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(complexTypePayload);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertFalse(result.isEmpty()),
            () -> assertTrue(result.get("Note").isObject()),
            () -> assertEquals(4, listAppender.list.size())
        );
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithCreateAndWithoutReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        service.restoreConditionalFields(caseTypeDefinition, newData, existingData, "123");

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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithCreateAndWithoutReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldDoNothingIfParentNodeIsMissing() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertEquals(0, listAppender.list.size());
    }

    @Test
    void shouldAddMissingSimpleFieldToNewDataRootNoteWithCreateAndWithoutReadPermission() {
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

        setupLogging().setLevel(Level.INFO);

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithCreateAndWithoutReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertTrue(result.get("Note").has("type")),
            () -> assertEquals("test", result.get("Note").get("type").asText()),
            () -> assertEquals("Adding missing field 'type' under 'Note'.",
              listAppender.list.getFirst().getFormattedMessage())
        );

    }

    @Test
    void shouldDoNothingIfNewDataRootNoteFieldIsEmptyWithoutCreateAndReadPermission() {
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

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(noteWithNestedFieldsWithoutCreateAndReadPermission()).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertEquals("Missing field 'type' under 'Note'.", listAppender.list.getFirst().getFormattedMessage());
    }

    @Test
    void shouldAddMissingUndefinedFieldInDocumentObjectWithCreateWithoutReadPermission() {
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

        String expectedMessage = "Adding missing field 'category_id' under 'documentLink'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(existingDataString, newDataString,
            generatedCaseDocumentsFieldWithCreateWithoutReadPermission(), Level.INFO, expectedMessage);

        assertAll(
            () -> assertTrue(result.get("generatedCaseDocuments").isArray()),
            () -> assertEquals(1, result.get("generatedCaseDocuments").size()),
            () -> assertEquals("123", result.get("generatedCaseDocuments").get(0).get("id").asText()),
            () -> assertEquals("Test",
                result.get("generatedCaseDocuments").get(0).get("value").get("createdBy").asText()),
            () -> assertTrue(result.get("generatedCaseDocuments").get(0).get("value").get("documentLink").has(
                "document_url")),
            () -> assertEquals("http_document_url",
                result.get("generatedCaseDocuments").get(0).get("value").get("documentLink")
                    .get("document_url").asText()),
            () -> assertTrue(result.get("generatedCaseDocuments").get(0)
                .get("value").get("documentLink").has("document_filename")),
            () -> assertEquals("document.pdf", result.get("generatedCaseDocuments").get(0)
                .get("value").get("documentLink").get("document_filename").asText()),
            () -> assertTrue(result.get("generatedCaseDocuments").get(0).get("value").get("documentLink")
                .has("document_binary_url")),
            () -> assertEquals("http://document/binary", result.get("generatedCaseDocuments").get(0)
                .get("value").get("documentLink").get("document_binary_url").asText()),
            () -> assertTrue(result.get("generatedCaseDocuments").get(0).get("value").get("documentLink")
                .has("category_id")),
            () -> assertEquals("detailsOfClaim", result.get("generatedCaseDocuments").get(0)
                .get("value").get("documentLink").get("category_id").asText()),
            () -> assertEquals("document.pdf", result.get("generatedCaseDocuments").get(0)
                .get("value").get("documentName").asText()),
            () -> assertEquals(34805, result.get("generatedCaseDocuments").get(0).get("value")
                .get("documentSize").asInt()),
            () -> assertEquals("CLAIM", result.get("generatedCaseDocuments").get(0).get("value")
                .get("documentType").asText()),
            () -> assertEquals("2024-10-16T10:47:03", result.get("generatedCaseDocuments").get(0)
                .get("value").get("createdDatetime").asText())
        );
    }

    @Test
    void shouldDoNothingWhenUndefinedFieldIsMissingInDocumentObjectWithoutCreateAndReadPermission() {
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

        String expectedMessage = "Missing field 'category_id' under 'documentLink'.";

        Map<String, JsonNode> result = assertFilterServiceAndLogging(existingDataString, newDataString,
            generatedCaseDocumentsFieldWithoutCreateAndReadPermission(), Level.DEBUG, expectedMessage);

        assertEquals(getJsonMapNode(newDataString), result);
    }

    private Logger setupLogging() {
        listAppender = new ListAppender<>();
        listAppender.start();
        logger = (Logger) LoggerFactory.getLogger(ConditionalFieldRestorer.class);
        logger.detachAndStopAllAppenders();
        if (loggingEventList != null && !loggingEventList.isEmpty()) {
            loggingEventList.clear();
        }
        logger.addAppender(listAppender);
        return logger;
    }

    private Map<String, JsonNode> getJsonMapNode(final String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, JsonNode.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, JsonNode> assertFilterServiceAndLogging(final String existingDataString,
                                                                final String newDataString,
                                                                CaseFieldDefinition field, Level expectedLogLevel,
                                                                String expectedLogMessage) {
        setupLogging().setLevel(expectedLogLevel);

        CaseTypeDefinition caseTypeDefinition =
            newCaseType().withField(field).build();

        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        loggingEventList = listAppender.list;

        assertAll(
            () -> assertTrue(loggingEventList.stream().anyMatch(log -> log.getLevel() == expectedLogLevel
                    && log.getFormattedMessage().equals(expectedLogMessage)),
                "Expected log message not found: " + expectedLogMessage)
        );

        return result;
    }

    private CaseTypeDefinition caseDefinitionWithNestedList() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String defContent  = fromFileAsString("tests/nested-list-definition.json");

            return objectMapper.readValue(defContent, CaseTypeDefinition.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CaseTypeDefinition caseTypeDefinitionWithNestedListWithCreatePermissionWithoutReadPermission() {
        AccessControlList controlList = new AccessControlList();
        controlList.setAccessProfile("caseworker-probate-loa1");
        controlList.setCreate(true);
        controlList.setRead(false);

        CaseTypeDefinition caseTypeDefinition = caseDefinitionWithNestedList();
        caseTypeDefinition.getCaseFieldDefinitions().getFirst().setAccessControlLists(List.of(controlList));
        caseTypeDefinition.getCaseFieldDefinitions().forEach(definition -> {
            definition.setAccessControlLists(List.of(controlList));
            definition.propagateACLsToNestedFields();
        });
        return caseTypeDefinition;
    }

    @Test
    void shouldAddMissingSingleCollectionFieldInNestedCollections() {
        final String existingDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [
             						{
             							"id": "fcf1d29b",
             							"value": "Party"
             						},
             						{
             							"id": "be4fcd2e",
             							"value": "Special measure"
             						}
             					],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        final String newDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [
             						{
             							"id": "be4fcd2e",
             							"value": "Special measure"
             						}
             					],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        setupLogging().setLevel(Level.INFO);

        CaseTypeDefinition caseTypeDefinition =
            caseTypeDefinitionWithNestedListWithCreatePermissionWithoutReadPermission();
        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertTrue(result.containsKey("applicant1Flags")),
            () -> assertTrue(result.get("applicant1Flags").has("details")),
            () -> {
                JsonNode details = result.get("applicant1Flags").get("details");
                assertTrue(details.isArray());
                assertTrue(details.get(0).has("value"));
                JsonNode path = details.get(0).get("value").get("path");
                assertEquals(2, path.size());
            },
            () -> assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("Adding missing collection item with ID '\"fcf1d29b\"' under 'path'.")))
        );
    }

    @Test
    void shouldAddMissingCollectionFieldsInNestedCollections() {
        final String existingDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [
             						{
             							"id": "fcf1d29b",
             							"value": "Party"
             						},
             						{
             							"id": "be4fcd2e",
             							"value": "Special measure"
             						}
             					],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        final String newDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        setupLogging().setLevel(Level.INFO);

        CaseTypeDefinition caseTypeDefinition =
            caseTypeDefinitionWithNestedListWithCreatePermissionWithoutReadPermission();
        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertTrue(result.containsKey("applicant1Flags")),
            () -> assertTrue(result.get("applicant1Flags").has("details")),
            () -> {
                JsonNode details = result.get("applicant1Flags").get("details");
                assertTrue(details.isArray());
                assertTrue(details.get(0).has("value"));
                JsonNode path = details.get(0).get("value").get("path");
                assertEquals(2, path.size());
            },
            () -> assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("Adding missing collection item with ID '\"fcf1d29b\"' under 'path'."))),
            () -> assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("Adding missing collection item with ID '\"be4fcd2e\"' under 'path'.")))
        );
    }

    @Test
    void shouldIgnoreMissingCollectionFieldsInNestedCollections() {
        final String existingDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [
             						{
             							"id": "fcf1d29b",
             							"value": "Party"
             						},
             						{
             							"id": "be4fcd2e",
             							"value": "Special measure"
             						}
             					],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        final String newDataString = """
            {
             	"applicant1Flags": {
             		"details": [
             			{
             				"id": "d84ee6ec",
             				"value": {
             					"name": "Removal of things",
             					"path": [],
             					"status": "Active"
             				}
             			}
             		]
             	}
            }
            """;

        setupLogging().setLevel(Level.DEBUG);

        CaseTypeDefinition caseTypeDefinition = caseDefinitionWithNestedList();
        Map<String, JsonNode> existingData = getJsonMapNode(existingDataString);
        Map<String, JsonNode> newData = getJsonMapNode(newDataString);

        Map<String, JsonNode> result = service.restoreConditionalFields(caseTypeDefinition, newData, existingData,
            "123");

        assertAll(
            () -> assertTrue(result.containsKey("applicant1Flags")),
            () -> assertTrue(result.get("applicant1Flags").has("details")),
            () -> {
                JsonNode details = result.get("applicant1Flags").get("details");
                assertTrue(details.isArray());
                assertTrue(details.get(0).has("value"));
                JsonNode path = details.get(0).get("value").get("path");
                assertEquals(0, path.size());
            },
            () -> assertEquals(2, listAppender.list.size()),
            () -> assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("Missing collection item with ID '\"fcf1d29b\"' under 'path'."))),
            () -> assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("Missing collection item with ID '\"be4fcd2e\"' under 'path'.")))
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();

        if (listAppender != null) {
            listAppender.stop();
        }
        if (logger != null) {
            logger.detachAndStopAllAppenders();
        }
    }
}
