package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CompoundFieldOrderServiceTest {

    private static final FieldType textFieldType = aFieldType().withId("Text").withType("Text").build();
    private static final CaseField CASE_FIELD = newCaseField()
        .withFieldType(textFieldType)
        .withId("PersonFirstName")
        .build();
    private static final String TEXT_TYPE = "Text";

    private CompoundFieldOrderService compoundFieldOrderService;

    @Nested
    @DisplayName("Field Type builder tests")
    class FieldTypeBuilderTest {

        @Nested
        @DisplayName("Standard Complex Fields Ordering (case type tab and event trigger)")
        class StandardComplexFieldsOrderingTest {


            @BeforeEach
            public void setUp() {
                compoundFieldOrderService = spy(new CompoundFieldOrderService());
            }

            private CaseField simpleField(final String id, final int order) {
                return newCaseField()
                    .withId(id)
                    .withFieldType(simpleType())
                    .withOrder(order)
                    .build();
            }

            private CaseField complexField(final String id, final int order) {
                return newCaseField()
                    .withId(id)
                    .withFieldType(complexType())
                    .withOrder(order)
                    .build();
            }

            private FieldType simpleType() {
                return aFieldType().withType(TEXT_TYPE).build();
            }

            private FieldType complexType() {
                return aFieldType().withType(COMPLEX).build();
            }

            @Test
            @DisplayName("should build field type for collection containing complex type of simple fields")
            void shouldOrderCaseFieldsInComplexTypeOfCollectionField() {
                FieldType collectionComplexFieldType = buildComplexOfSimpleFieldType("One", "Two", "Three");

                CASE_FIELD.setFieldType(aFieldType()
                                            .withType(COLLECTION)
                                            .withCollectionFieldType(collectionComplexFieldType)
                                            .build());

                compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), "");

                List<CaseField> complexFields = CASE_FIELD.getFieldType().getCollectionFieldType().getComplexFields();
                hasCorrectComplexField(complexFields, Lists.newArrayList("Three", "Two", "One"));
            }

            @Test
            @DisplayName("should order fields on certain level regardless of what field type they are")
            void shouldOrderFieldsRegardlessOfWhatFieldTypeTheyAre() {
                FieldType complexOfSimpleFieldTypes1 = buildComplexOfSimpleFieldType("One", "Two", "Three");
                FieldType complexOfComplexFieldTypes2 = buildComplexOfComplexFieldType("Four", "Five", "Six");
                FieldType complexOfSimpleFieldTypes3 = buildComplexOfSimpleFieldType("Seven", "Eight", "Nine");

                CASE_FIELD.setFieldType(aFieldType()
                                            .withType(COMPLEX)
                                            .withComplexField(newCaseField().withFieldType(complexOfSimpleFieldTypes1).build())
                                            .withComplexField(newCaseField().withFieldType(complexOfComplexFieldTypes2).build())
                                            .withComplexField(newCaseField().withFieldType(complexOfSimpleFieldTypes3).build())
                                            .build());

                compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), "");

                List<CaseField> complexFields = CASE_FIELD.getFieldType().getComplexFields();
                List<CaseField> complexFields1 = complexFields.get(0).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields1, Lists.newArrayList("Three", "Two", "One"));
                List<CaseField> complexFields2 = complexFields.get(1).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields2, Lists.newArrayList("Six", "Five", "Four"));
                List<CaseField> complexFields3 = complexFields.get(2).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields3, Lists.newArrayList("Nine", "Eight", "Seven"));
            }

            @Test
            @DisplayName("should build field type for collection containing complex type of complex types of simple fields")
            void shouldOrderCaseFieldsInComplexTypesOfComplexTypeOfCollectionField() {
                FieldType complexOfComplexFieldType1 = buildComplexOfSimpleFieldType("One", "Two", "Three");
                FieldType complexOfComplexFieldType2 = buildComplexOfSimpleFieldType("Four", "Five", "Six");
                FieldType complexOfComplexFieldType3 = buildComplexOfSimpleFieldType("Seven", "Eight", "Nine");
                FieldType collectionComplexFieldType = aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(newCaseField().withFieldType(complexOfComplexFieldType1).build())
                    .withComplexField(newCaseField().withFieldType(complexOfComplexFieldType2).build())
                    .withComplexField(newCaseField().withFieldType(complexOfComplexFieldType3).build())
                    .build();

                CASE_FIELD.setFieldType(aFieldType()
                                            .withType(COLLECTION)
                                            .withCollectionFieldType(collectionComplexFieldType)
                                            .build());

                compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), "");

                List<CaseField> complexFields = CASE_FIELD.getFieldType().getCollectionFieldType().getComplexFields();
                List<CaseField> complexFields1 = complexFields.get(0).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields1, Lists.newArrayList("Three", "Two", "One"));
                List<CaseField> complexFields2 = complexFields.get(1).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields2, Lists.newArrayList("Six", "Five", "Four"));
                List<CaseField> complexFields3 = complexFields.get(2).getFieldType().getComplexFields();
                hasCorrectComplexField(complexFields3, Lists.newArrayList("Nine", "Eight", "Seven"));
            }

            private FieldType buildComplexOfComplexFieldType(final String id1, final String id2, final String id3) {

                return aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(complexField(id1, 3))
                    .withComplexField(complexField(id2, 2))
                    .withComplexField(complexField(id3, 1))
                    .build();
            }

            private FieldType buildComplexOfSimpleFieldType(final String id1, final String id2, final String id3) {
                return aFieldType()
                    .withType(COMPLEX)
                    .withComplexField(simpleField(id1, 3))
                    .withComplexField(simpleField(id2, 2))
                    .withComplexField(simpleField(id3, 1))
                    .build();
            }

            private void hasCorrectComplexField(final List<CaseField> complexFields, final List<String> ids) {
                for (int index = 0; index < ids.size(); index++) {
                    assertThat(complexFields.get(index), hasCorrectCaseField(ids.get(index), index + 1));
                }
            }

            private Matcher<? super CaseField> hasCorrectCaseField(String id, int order) {
                return allOf(
                    hasProperty("id", is(id)),
                    hasProperty("order", is(order)));
            }
        }

        @Nested
        @DisplayName("Event To Complex Types Complex Fields Ordering (event trigger only)")
        class EventToComplexTypesComplexFieldsOrderingTest {

        }
    }
}
