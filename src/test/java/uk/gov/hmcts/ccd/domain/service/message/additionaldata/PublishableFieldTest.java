package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class PublishableFieldTest {

    private PublishableField publishableField;

    @BeforeEach
    void setUp() {
        publishableField = new PublishableField();
    }

    @Nested
    class ConstructorTest {

        private CaseTypeDefinition caseTypeDefinition;
        private CaseEventFieldDefinition caseEventFieldDefinition;
        private CaseEventFieldComplexDefinition caseEventFieldComplexDefinition;
        private CaseDetails caseDetails;

        private static final String FIELD_ID = "FieldId";
        private static final String FIELD_ALIAS = "Alias";
        private static final String NESTED_FIELD = "NestedField";
        private static final String FULL_PATH = FIELD_ID + "." + NESTED_FIELD;

        @Test
        void shouldCreatePublishableFieldForTopLevelField() {
            caseTypeDefinition = newCaseType()
                .withCaseFields(List.of(
                    newCaseField()
                        .withId(FIELD_ID)
                        .withFieldType(textField())
                        .build()
                ))
                .build();

            caseEventFieldDefinition = newCaseEventField()
                .withCaseFieldId(FIELD_ID)
                .withDisplayContext(DisplayContext.MANDATORY)
                .build();

            caseDetails = newCaseDetails()
                .withData(newHashMap())
                .build();

            PublishableField result = new PublishableField(caseTypeDefinition, caseEventFieldDefinition, caseDetails);

            assertAll(
                () -> assertThat(result.getKey(), is(FIELD_ID)),
                () -> assertThat(result.getPath(), is(FIELD_ID)),
                () -> assertThat(result.getOriginalId(), is(FIELD_ID)),
                () -> assertThat(result.getDisplayContext(), is(DisplayContext.MANDATORY)),
                () -> assertThat(result.getFieldType().getType(), is(TEXT)),
                () -> assertThat(result.getCaseField().getId(), is(FIELD_ID)),
                () -> assertThat(result.isPublishTopLevel(), is(true))
            );
        }

        @Test
        void shouldCreatePublishableFieldForTopLevelFieldWithAlias() {
            caseTypeDefinition = newCaseType()
                .withCaseFields(List.of(
                    newCaseField()
                        .withId(FIELD_ID)
                        .withFieldType(textField())
                        .build()
                ))
                .build();

            caseEventFieldDefinition = newCaseEventField()
                .withCaseFieldId(FIELD_ID)
                .withDisplayContext(DisplayContext.MANDATORY)
                .withPublishAs(FIELD_ALIAS)
                .build();

            caseDetails = newCaseDetails()
                .withData(newHashMap())
                .build();

            PublishableField result = new PublishableField(caseTypeDefinition, caseEventFieldDefinition, caseDetails);

            assertAll(
                () -> assertThat(result.getKey(), is(FIELD_ALIAS)),
                () -> assertThat(result.getPath(), is(FIELD_ID)),
                () -> assertThat(result.getOriginalId(), is(FIELD_ID)),
                () -> assertThat(result.getDisplayContext(), is(DisplayContext.MANDATORY)),
                () -> assertThat(result.getFieldType().getType(), is(TEXT)),
                () -> assertThat(result.getCaseField().getId(), is(FIELD_ID)),
                () -> assertThat(result.isPublishTopLevel(), is(true))
            );
        }

        @Test
        void shouldCreatePublishableFieldForNestedField() {
            caseTypeDefinition = newCaseType()
                .withCaseFields(List.of(
                    newCaseField()
                        .withId(FIELD_ID)
                        .withFieldType(
                            aFieldType()
                                .withId("SomeComplexType")
                                .withType(COMPLEX)
                                .withComplexField(
                                    newCaseField()
                                        .withId(NESTED_FIELD)
                                        .withFieldType(textField())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                ))
                .build();

            caseEventFieldComplexDefinition = CaseEventFieldComplexDefinition.builder()
                .reference(NESTED_FIELD)
                .build();

            caseDetails = newCaseDetails()
                .withData(newHashMap())
                .build();

            PublishableField result = new PublishableField(caseTypeDefinition, caseEventFieldComplexDefinition,
                FULL_PATH, caseDetails);

            assertAll(
                () -> assertThat(result.getKey(), is(NESTED_FIELD)),
                () -> assertThat(result.getPath(), is(FULL_PATH)),
                () -> assertThat(result.getOriginalId(), is(NESTED_FIELD)),
                () -> assertThat(result.getFieldType().getType(), is(TEXT)),
                () -> assertThat(result.getCaseField().getId(), is(NESTED_FIELD)),
                () -> assertThat(result.isPublishTopLevel(), is(false))
            );
        }

        @Test
        void shouldCreatePublishableFieldForNestedFieldWithAlias() {
            caseTypeDefinition = newCaseType()
                .withCaseFields(List.of(
                    newCaseField()
                        .withId(FIELD_ID)
                        .withFieldType(
                            aFieldType()
                                .withId("SomeComplexType")
                                .withType(COMPLEX)
                                .withComplexField(
                                    newCaseField()
                                        .withId(NESTED_FIELD)
                                        .withFieldType(textField())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                ))
                .build();

            caseEventFieldComplexDefinition = CaseEventFieldComplexDefinition.builder()
                .reference(NESTED_FIELD)
                .publishAs(FIELD_ALIAS)
                .build();

            caseDetails = newCaseDetails()
                .withData(newHashMap())
                .build();

            PublishableField result = new PublishableField(caseTypeDefinition, caseEventFieldComplexDefinition,
                FULL_PATH, caseDetails);

            assertAll(
                () -> assertThat(result.getKey(), is(FIELD_ALIAS)),
                () -> assertThat(result.getPath(), is(FULL_PATH)),
                () -> assertThat(result.getOriginalId(), is(NESTED_FIELD)),
                () -> assertThat(result.getFieldType().getType(), is(TEXT)),
                () -> assertThat(result.getCaseField().getId(), is(NESTED_FIELD)),
                () -> assertThat(result.isPublishTopLevel(), is(true))
            );
        }

        @Test
        void shouldThrowExceptionWhenFieldIdCannotBeFoundInCaseType() {
            caseTypeDefinition = newCaseType()
                .withCaseTypeId("CaseTypeId")
                .withCaseFields(List.of(
                    newCaseField()
                        .withId(FIELD_ID)
                        .withFieldType(textField())
                        .build()
                ))
                .build();

            caseEventFieldDefinition = newCaseEventField()
                .withCaseFieldId("UnknownId")
                .withDisplayContext(DisplayContext.MANDATORY)
                .build();

            caseDetails = newCaseDetails()
                .withData(newHashMap())
                .build();

            ServiceException exception = assertThrows(ServiceException.class, () ->
                new PublishableField(caseTypeDefinition, caseEventFieldDefinition, caseDetails));

            assertThat(exception.getMessage(),
                is("Case event field 'UnknownId' cannot be found in configuration for case type 'CaseTypeId'."));
        }

        private FieldTypeDefinition textField() {
            return aFieldType()
                .withId(TEXT)
                .withType(TEXT)
                .build();
        }
    }

    @Nested
    class IsNestedTest {

        @Test
        void shouldReturnTrueForNestedField() {
            publishableField.setPath("TopLevel.NestedLevel");

            boolean result = publishableField.isNested();

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseForTopLevelField() {
            publishableField.setPath("TopLevel");

            boolean result = publishableField.isNested();

            assertFalse(result);
        }
    }

    @Nested
    class GetFieldIdTest {

        @Test
        void shouldGetFieldIdForFieldWithNestedPath() {
            publishableField.setPath("TopLevel.NestedLevel.SubNestedLevel");

            String result = publishableField.getFieldId();

            assertEquals("SubNestedLevel", result);
        }

        @Test
        void shouldGetFieldIdForFieldWithNonNestedPath() {
            publishableField.setPath("TopLevel");
            publishableField.setCaseField(newCaseField().withId("TopLevel").build());

            String result = publishableField.getFieldId();

            assertEquals("TopLevel", result);
        }
    }

    @Nested
    class SplitPathTest {

        @Test
        void shouldSplitPathForFieldWithNestedPath() {
            publishableField.setPath("TopLevel.NestedLevel.SubNestedLevel");

            String[] result = publishableField.splitPath();

            assertEquals(3, result.length);
            assertEquals("TopLevel", result[0]);
            assertEquals("NestedLevel", result[1]);
            assertEquals("SubNestedLevel", result[2]);
        }

        @Test
        void shouldSplitPathForFieldWithNonNestedPath() {
            publishableField.setPath("TopLevel");

            String[] result = publishableField.splitPath();

            assertEquals(1, result.length);
            assertEquals("TopLevel", result[0]);
        }
    }

    @Nested
    class IsSubfieldOfTest {

        private PublishableField comparisonField;

        @BeforeEach
        void setUp() {
            comparisonField = new PublishableField();
        }

        @Test
        void shouldReturnTrueWhenFieldIsADirectSubfield() {
            publishableField.setPath("TopLevel.NestedLevel.SubNestedLevel");
            comparisonField.setPath("TopLevel.NestedLevel");

            boolean result = publishableField.isSubFieldOf(comparisonField);

            assertTrue(result);
        }

        @Test
        void shouldReturnTrueWhenFieldIsADeepNestedSubfield() {
            publishableField.setPath("TopLevel.NestedLevel.SubNestedLevel.AnotherLevel");
            comparisonField.setPath("TopLevel");

            boolean result = publishableField.isSubFieldOf(comparisonField);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenFieldIsAParent() {
            publishableField.setPath("TopLevel.NestedLevel");
            comparisonField.setPath("TopLevel.NestedLevel.SubNestedLevel");

            boolean result = publishableField.isSubFieldOf(comparisonField);

            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenFieldIsASibling() {
            publishableField.setPath("TopLevel.NestedLevel");
            comparisonField.setPath("TopLevel.OtherNestedField");

            boolean result = publishableField.isSubFieldOf(comparisonField);

            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenFieldIdsAreSimilar() {
            publishableField.setPath("TopLevel.NestedLevel.SubNestedLevel");
            comparisonField.setPath("TopLevel.NestedLevel2");

            boolean result = publishableField.isSubFieldOf(comparisonField);

            assertFalse(result);
        }
    }

    @Nested
    class FilterDirectChildrenFromTest {

        @Test
        void shouldOnlyReturnDirectChildren() {
            publishableField.setPath("Field");

            PublishableField field1 = PublishableField.builder().path("Field.NestedField").build();
            PublishableField field2 = PublishableField.builder().path("Field.NestedField.SubNestedField").build();
            PublishableField field3 = PublishableField.builder().path("Field.OtherNestedField").build();
            PublishableField field4 = PublishableField.builder().path("OtherField.OtherNestedField").build();
            PublishableField field5 = PublishableField.builder().path("FieldX.NestedField").build();
            PublishableField field6 = PublishableField.builder().path("Field").build();

            List<PublishableField> result = publishableField.filterDirectChildrenFrom(List.of(
                field1, field2, field3, field4, field5, field6
            ));

            assertEquals(2, result.size());
            assertEquals(field1, result.get(0));
            assertEquals(field3, result.get(1));
        }
    }
}