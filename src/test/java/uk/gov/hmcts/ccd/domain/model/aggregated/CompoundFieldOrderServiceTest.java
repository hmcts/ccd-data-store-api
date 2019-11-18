package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService.ROOT;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex.builder;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.FIXED_LIST;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.FIXED_RADIO_LIST;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.MULTI_SELECT_LIST;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FixedListItemBuilder.aFixedListItem;

class CompoundFieldOrderServiceTest {

    private static final FieldType textFieldType = aFieldType().withId("Text").withType("Text").build();
    private static final CaseField CASE_FIELD = newCaseField()
        .withFieldType(textFieldType)
        .withId("PersonFirstName")
        .build();
    private static final String TEXT_TYPE = "Text";

    private CompoundFieldOrderService compoundFieldOrderService = new CompoundFieldOrderService();

    @Nested
    @DisplayName("Case Field ordering")
    class CaseFieldOrderingTest {

        @Test
        @DisplayName("should build field type for collection containing complex type of simple fields")
        void shouldOrderCaseFieldsAsOneOfFixedListTypes() {
            FieldType multiSelectFixedListType = aFieldType()
                .withType(MULTI_SELECT_LIST)
                .withFixedListItems(fixedListItem("item3", 2),
                                    fixedListItem("item1", 3),
                                    fixedListItem("item2", 1))
                .build();
            CASE_FIELD.setFieldType(multiSelectFixedListType);

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), ROOT);

            List<FixedListItem> fixedListItems = CASE_FIELD.getFieldType().getFixedListItems();
            assertThat(fixedListItems, contains(hasProperty("code", is("item2")),
                                                hasProperty("code", is("item3")),
                                                hasProperty("code", is("item1"))));
        }

        @Test
        @DisplayName("should build field type for collection containing complex type of simple fields")
        void shouldOrderCaseFieldsInComplexTypeOfCollectionField() {
            FieldType collectionComplexFieldType = aFieldType()
                .withType(COMPLEX)
                .withComplexField(complexField("One", 5))
                .withComplexField(simpleMultiSelectListField("Two", 4,
                                                             fixedListItem("item3", 2),
                                                             fixedListItem("item1", 3),
                                                             fixedListItem("item2", 1)))
                .withComplexField(simpleFixedRadioListField("Five", 1,
                                                            fixedListItem("item6", 3),
                                                            fixedListItem("item5", 2),
                                                            fixedListItem("item4", 1)))
                .withComplexField(complexField("Three", 3))
                .withComplexField(simpleFixedListField("Four", 2,
                                                       fixedListItem("item9", 1),
                                                       fixedListItem("item7", 3),
                                                       fixedListItem("item8", 2)))
                .build();

            CASE_FIELD.setFieldType(aFieldType()
                                        .withType(COLLECTION)
                                        .withCollectionFieldType(collectionComplexFieldType)
                                        .build());

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), ROOT);

            List<CaseField> complexFields = CASE_FIELD.getFieldType().getCollectionFieldType().getComplexFields();
            assertThat(complexFields, contains(allOf(hasProperty("id", is("Five")),
                                                     hasProperty("order", is(1)),
                                                     hasProperty("fieldType",
                                                                 hasProperty("fixedListItems",
                                                                             contains(hasProperty("code", is("item4")),
                                                                                      hasProperty("code", is("item5")),
                                                                                      hasProperty("code", is("item6")))))),
                                               allOf(hasProperty("id", is("Four")),
                                                     hasProperty("order", is(2)),
                                                     hasProperty("fieldType",
                                                                 hasProperty("fixedListItems",
                                                                             contains(hasProperty("code", is("item9")),
                                                                                      hasProperty("code", is("item8")),
                                                                                      hasProperty("code", is("item7")))))),
                                               allOf(hasProperty("id", is("Three")),
                                                     hasProperty("order", is(3))),
                                               allOf(hasProperty("id", is("Two")),
                                                     hasProperty("order", is(4)),
                                                     hasProperty("fieldType",
                                                                 hasProperty("fixedListItems",
                                                                             contains(hasProperty("code", is("item2")),
                                                                                      hasProperty("code", is("item3")),
                                                                                      hasProperty("code", is("item1")))))),
                                               allOf(hasProperty("id", is("One")),
                                                     hasProperty("order", is(5)))));
        }

        @Test
        @DisplayName("should build field type for complex containing complex type of simple fields")
        void shouldOrderCaseFieldsInComplexTypesOfComplexTypeOfComplexField() {
            FieldType complexOfComplexFieldType1 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(complexField("One", 3))
                .withComplexField(complexField("Two", 2))
                .withComplexField(complexField("Three", 1))
                .build();
            FieldType complexOfComplexFieldType2 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(complexField("Four", 3))
                .withComplexField(complexField("Five", 2))
                .withComplexField(complexField("Six", 1))
                .build();
            FieldType complexOfComplexFieldType3 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(complexField("Seven", 3))
                .withComplexField(complexField("Eight", 2))
                .withComplexField(complexField("Nine", 1))
                .build();
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

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), ROOT);

            List<CaseField> complexFields = CASE_FIELD.getFieldType().getCollectionFieldType().getComplexFields();
            assertThat(complexFields.get(0).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Three")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Two")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("One")),
                                                                                         hasProperty("order", is(3)))));
            assertThat(complexFields.get(1).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Six")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Five")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("Four")),
                                                                                         hasProperty("order", is(3)))));
            assertThat(complexFields.get(2).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Nine")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Eight")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("Seven")),
                                                                                         hasProperty("order", is(3)))));
        }

        @Test
        @DisplayName("should order fields on different level regardless of what field type they are")
        void shouldOrderFieldsRegardlessOfWhatFieldTypeTheyAre() {
            FieldType complexOfSimpleFieldTypes1 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(simpleField("One", 3))
                .withComplexField(simpleField("Two", 2))
                .withComplexField(simpleField("Three", 1))
                .build();
            FieldType complexOfComplexFieldTypes2 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(complexField("Four", 3))
                .withComplexField(complexField("Five", 2))
                .withComplexField(complexField("Six", 1))
                .build();
            FieldType complexOfSimpleFieldTypes3 = aFieldType()
                .withType(COMPLEX)
                .withComplexField(simpleField("Seven", 3))
                .withComplexField(simpleField("Eight", 2))
                .withComplexField(simpleField("Nine", 1))
                .build();

            CASE_FIELD.setFieldType(aFieldType()
                                        .withType(COMPLEX)
                                        .withComplexField(newCaseField().withFieldType(complexOfSimpleFieldTypes1).build())
                                        .withComplexField(newCaseField().withFieldType(complexOfComplexFieldTypes2).build())
                                        .withComplexField(newCaseField().withFieldType(complexOfSimpleFieldTypes3).build())
                                        .build());

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, Lists.newArrayList(), ROOT);

            List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
            assertThat(complexFields.get(0).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Three")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Two")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("One")),
                                                                                         hasProperty("order", is(3)))));
            assertThat(complexFields.get(1).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Six")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Five")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("Four")),
                                                                                         hasProperty("order", is(3)))));
            assertThat(complexFields.get(2).getFieldType().getChildren(), contains(allOf(hasProperty("id", is("Nine")),
                                                                                         hasProperty("order", is(1))),
                                                                                   allOf(hasProperty("id", is("Eight")),
                                                                                         hasProperty("order", is(2))),
                                                                                   allOf(hasProperty("id", is("Seven")),
                                                                                         hasProperty("order", is(3)))));
        }
    }

    @Nested
    @DisplayName("Event To Complex Types Complex Fields ordering")
    class EventToComplexTypesComplexFieldsOrderingTest {

        @Test
        @DisplayName("should override top level case fields order from case event fields order")
        void shouldOverrideTopLevelCaseFieldsOrderFromCaseEventFieldsOrder() {
            FieldType collectionComplexFieldType = aFieldType()
                .withType(COMPLEX)
                .withComplexField(simpleField("One", 2))
                .withComplexField(simpleField("Two", 3))
                .withComplexField(simpleField("Three", 1))
                .build();

            CASE_FIELD.setFieldType(aFieldType()
                                        .withType(COLLECTION)
                                        .withCollectionFieldType(collectionComplexFieldType)
                                        .build());

            List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(builder().reference("One").order(3).build(),
                                                                                    builder().reference("Two").order(2).build(),
                                                                                    builder().reference("Three").order(1).build());

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, caseEventComplexFields, ROOT);

            List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
            assertThat(complexFields, hasItems(allOf(hasProperty("id", is("Three")),
                                                     hasProperty("order", is(1))),
                                               allOf(hasProperty("id", is("Two")),
                                                     hasProperty("order", is(3))),
                                               allOf(hasProperty("id", is("One")),
                                                     hasProperty("order", is(2)))));
        }

        @Test
        @DisplayName("should override deeper level case fields order from case event fields order with missing order values")
        void shouldOverrideDeeperLevelCaseFieldsOrderFromCaseEventFieldsOrder() {
            FieldType complexOfSimpleTypes = aFieldType()
                .withType(COMPLEX)
                .withComplexField(simpleField("simple1", 3))
                .withComplexField(simpleField("simple2", 4))
                .withComplexField(simpleField("simple3", 5))
                .withComplexField(simpleField("simple4", 1))
                .withComplexField(simpleField("simple5", 2))
                .build();
            FieldType multipleNestedCompoundFieldType = aFieldType()
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                                      .withId("complex1")
                                      .withFieldType(aFieldType()
                                                         .withType(COLLECTION)
                                                         .withCollectionFieldType(aFieldType()
                                                                                      .withType(COMPLEX)
                                                                                      .withComplexField(newCaseField()
                                                                                                            .withId("complex2")
                                                                                                            .withFieldType(complexOfSimpleTypes)
                                                                                                            .withOrder(1)
                                                                                                            .build())
                                                                                      .build())
                                                         .build())
                                      .withOrder(2)
                                      .build())
                .withComplexField(newCaseField()
                                      .withId("complex3")
                                      .withFieldType(aFieldType()
                                                         .withType(COMPLEX)
                                                         .withComplexField(simpleField("simple6", 4))
                                                         .withComplexField(simpleField("simple7", 3))
                                                         .withComplexField(simpleField("simple8", 2))
                                                         .withComplexField(simpleField("simple9", 1))
                                                         .withComplexField(simpleField("simple10", 5))
                                                         .build())
                                      .withOrder(1)
                                      .build())
                .build();

            CASE_FIELD.setFieldType(aFieldType()
                                        .withType(COLLECTION)
                                        .withCollectionFieldType(multipleNestedCompoundFieldType)
                                        .build());

            List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(
                builder().reference("complex1.complex2.simple1").order(3).build(),
                builder().reference("complex1.complex2.simple2").order(2).build(),
                builder().reference("complex1.complex2.simple3").order(1).build(),
                builder().reference("complex1.complex2.simple5").order(4).build(),
                builder().reference("complex3.simple10").order(4).build(),
                builder().reference("complex3.simple8").order(2).build(),
                builder().reference("complex3.simple7").order(1).build());

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, caseEventComplexFields, ROOT);

            List<CaseField> complexChildren = CASE_FIELD.getFieldType().getChildren();
            List<CaseField> complex1Children = complexChildren.get(1).getFieldType().getChildren();
            List<CaseField> complex3Children = complexChildren.get(0).getFieldType().getChildren();
            assertThat(complex3Children, contains(allOf(hasProperty("id", is("simple7")),
                                                        hasProperty("order", is(3))),
                                                  allOf(hasProperty("id", is("simple8")),
                                                        hasProperty("order", is(2))),
                                                  allOf(hasProperty("id", is("simple10")),
                                                        hasProperty("order", is(5))),
                                                  allOf(hasProperty("id", is("simple6")),
                                                        hasProperty("order", is(4))),
                                                  allOf(hasProperty("id", is("simple9")),
                                                        hasProperty("order", is(1)))));
            List<CaseField> complex2Children = complex1Children.get(0).getFieldType().getChildren();
            assertThat(complex2Children, contains(allOf(hasProperty("id", is("simple3")),
                                                        hasProperty("order", is(5))),
                                                  allOf(hasProperty("id", is("simple2")),
                                                        hasProperty("order", is(4))),
                                                  allOf(hasProperty("id", is("simple1")),
                                                        hasProperty("order", is(3))),
                                                  allOf(hasProperty("id", is("simple5")),
                                                        hasProperty("order", is(2))),
                                                  allOf(hasProperty("id", is("simple4")),
                                                        hasProperty("order", is(1)))));
        }

        @Test
        @DisplayName("should override deeper level case fields order only if at least one case event fields exist")
        void shouldOverrideDeeperLevelCaseFieldsOrderIfCaseEventFieldsOrderExists() {
            FieldType complexOfSimpleTypes = aFieldType()
                .withType(COMPLEX)
                .withComplexField(simpleField("simple1", 3))
                .withComplexField(simpleField("simple2", 4))
                .withComplexField(simpleField("simple3", 5))
                .withComplexField(simpleField("simple4", 1))
                .withComplexField(simpleField("simple5", 2))
                .build();
            FieldType multipleNestedCompoundFieldType = aFieldType()
                .withType(COMPLEX)
                .withComplexField(newCaseField()
                                      .withId("complex1")
                                      .withFieldType(aFieldType()
                                                         .withType(COLLECTION)
                                                         .withCollectionFieldType(aFieldType()
                                                                                      .withType(COMPLEX)
                                                                                      .withComplexField(newCaseField()
                                                                                                            .withId("complex2")
                                                                                                            .withFieldType(complexOfSimpleTypes)
                                                                                                            .withOrder(1)
                                                                                                            .build())
                                                                                      .build())
                                                         .build())
                                      .withOrder(2)
                                      .build())
                .withComplexField(newCaseField()
                                      .withId("complex3")
                                      .withFieldType(aFieldType()
                                                         .withType(COMPLEX)
                                                         .withComplexField(simpleField("simple6", 4))
                                                         .withComplexField(simpleField("simple7", 3))
                                                         .withComplexField(simpleField("simple8", 2))
                                                         .withComplexField(simpleField("simple9", 1))
                                                         .withComplexField(simpleField("simple10", 5))
                                                         .build())
                                      .withOrder(1)
                                      .build())
                .build();

            CASE_FIELD.setFieldType(aFieldType()
                                        .withType(COLLECTION)
                                        .withCollectionFieldType(multipleNestedCompoundFieldType)
                                        .build());

            List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(
                builder().reference("complex1.complex2.simple3").order(3).build());

            compoundFieldOrderService.sortNestedFields(CASE_FIELD, caseEventComplexFields, ROOT);

            List<CaseField> complexChildren = CASE_FIELD.getFieldType().getChildren();
            List<CaseField> complex1Children = complexChildren.get(1).getFieldType().getChildren();
            List<CaseField> complex3Children = complexChildren.get(0).getFieldType().getChildren();
            assertThat(complex3Children, contains(allOf(hasProperty("id", is("simple9")),
                                                        hasProperty("order", is(1))),
                                                  allOf(hasProperty("id", is("simple8")),
                                                        hasProperty("order", is(2))),
                                                  allOf(hasProperty("id", is("simple7")),
                                                        hasProperty("order", is(3))),
                                                  allOf(hasProperty("id", is("simple6")),
                                                        hasProperty("order", is(4))),
                                                  allOf(hasProperty("id", is("simple10")),
                                                        hasProperty("order", is(5)))));
            List<CaseField> complex2Children = complex1Children.get(0).getFieldType().getChildren();
            assertThat(complex2Children, contains(allOf(hasProperty("id", is("simple3")),
                                                        hasProperty("order", is(5))),
                                                  allOf(hasProperty("id", is("simple1")),
                                                        hasProperty("order", is(3))),
                                                  allOf(hasProperty("id", is("simple2")),
                                                        hasProperty("order", is(4))),
                                                  allOf(hasProperty("id", is("simple4")),
                                                        hasProperty("order", is(1))),
                                                  allOf(hasProperty("id", is("simple5")),
                                                        hasProperty("order", is(2)))));
        }
    }

    private Matcher<? super CaseField> hasCorrectCaseField(String id, int order) {
        return allOf(
            hasProperty("id", is(id)),
            hasProperty("order", is(order)));
    }

    private FixedListItem fixedListItem(final String value, final int order) {
        return aFixedListItem().withCode(value).withOrder(String.valueOf(order)).build();
    }

    private CaseField simpleMultiSelectListField(final String id, final Integer order, FixedListItem... fixedListItems) {
        return newCaseField()
            .withId(id)
            .withFieldType(multiSelectListType(Lists.newArrayList(fixedListItems)))
            .withOrder(order)
            .build();
    }

    private CaseField simpleFixedRadioListField(final String id, final Integer order, FixedListItem... fixedListItems) {
        return newCaseField()
            .withId(id)
            .withFieldType(fixedRadioListType(Lists.newArrayList(fixedListItems)))
            .withOrder(order)
            .build();
    }

    private CaseField simpleFixedListField(final String id, final Integer order, FixedListItem... fixedListItems) {
        return newCaseField()
            .withId(id)
            .withFieldType(fixedListType(Lists.newArrayList(fixedListItems)))
            .withOrder(order)
            .build();
    }

    private CaseField simpleField(final String id, final Integer order) {
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

    private FieldType fixedListType(final List<FixedListItem> fixedListItems) {
        return aFieldType().withType(FIXED_LIST).withFixedListItems(fixedListItems).build();
    }

    private FieldType fixedRadioListType(final List<FixedListItem> fixedListItems) {
        return aFieldType().withType(FIXED_RADIO_LIST).withFixedListItems(fixedListItems).build();
    }

    private FieldType multiSelectListType(final List<FixedListItem> fixedListItems) {
        return aFieldType().withType(MULTI_SELECT_LIST).withFixedListItems(fixedListItems).build();
    }

    private FieldType simpleType() {
        return aFieldType().withType(TEXT_TYPE).build();
    }

    private FieldType complexType() {
        return aFieldType().withType(COMPLEX).build();
    }
}
