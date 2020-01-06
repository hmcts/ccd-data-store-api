package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService.ROOT;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex.builder;
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
    public static final ArrayList<CaseEventFieldComplex> EMPTY_CASE_EVENT_COMPLEX_FIELDS = Lists.newArrayList();

    private CompoundFieldOrderService compoundFieldOrderService = new CompoundFieldOrderService();

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields null")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsNull() {
        FieldType collectionComplexFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("Three", 1))
            .withComplexField(simpleField("One", 2))
            .withComplexField(simpleField("Two", 3))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(collectionComplexFieldType)
                                    .build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, null, ROOT);

        List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields empty")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsEmpty() {
        FieldType collectionComplexFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("Three", 1))
            .withComplexField(simpleField("One", 2))
            .withComplexField(simpleField("Two", 3))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(collectionComplexFieldType)
                                    .build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, EMPTY_CASE_EVENT_COMPLEX_FIELDS, ROOT);

        List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields not matching on simple type")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsNotMatching() {
        FieldType collectionComplexFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("Three", 1))
            .withComplexField(simpleField("One", 2))
            .withComplexField(simpleField("Two", 3))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(collectionComplexFieldType)
                                    .build());

        List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(builder().reference("OneTwo").order(3).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, caseEventComplexFields, ROOT);

        List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields matching but no order")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsMatchingButNoOrder() {
        FieldType collectionComplexFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("Three", 1))
            .withComplexField(simpleField("One", 2))
            .withComplexField(simpleField("Two", 3))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(collectionComplexFieldType)
                                    .build());

        List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(builder().reference("One").order(null).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, caseEventComplexFields, ROOT);

        List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should override top level case fields order from case event complex fields order even if duplicates exist")
    void shouldOverrideTopLevelCaseFieldsOrderFromCaseEventComplexFieldsOrderEvenIfDuplicatesExist() {
        FieldType collectionComplexFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("One", 1))
            .withComplexField(simpleField("Two", 2))
            .withComplexField(simpleField("Three", 3))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(collectionComplexFieldType)
                                    .build());

        List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(builder().reference("One").order(5).build(),
                                                                                builder().reference("One").order(6).build(),
                                                                                builder().reference("Two").order(3).build(),
                                                                                builder().reference("Two").order(4).build(),
                                                                                builder().reference("Three").order(1).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, caseEventComplexFields, ROOT);

        List<CaseField> complexFields = CASE_FIELD.getFieldType().getChildren();
        assertThat(complexFields, is(hasSize(3)));
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(3))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(1)))));
    }

    @Test
    @DisplayName("should override deeper level case fields order from case event fields order with missing order values")
    void shouldOverrideDeeperLevelCaseFieldsOrderFromCaseEventFieldsOrder() {
        FieldType complexOfSimpleTypes = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("simple4", 1))
            .withComplexField(simpleField("simple5", 2))
            .withComplexField(simpleField("simple1", 3))
            .withComplexField(simpleField("simple2", 4))
            .withComplexField(simpleField("simple3", 5))
            .build();
        FieldType multipleNestedCompoundFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                                  .withId("complex1")
                                  .withFieldType(aFieldType()
                                                     .withType(COMPLEX)
                                                     .withComplexField(simpleField("simple9", 1))
                                                     .withComplexField(simpleField("simple8", 2))
                                                     .withComplexField(simpleField("simple7", 3))
                                                     .withComplexField(simpleField("simple6", 4))
                                                     .withComplexField(simpleField("simple10", 5))
                                                     .build())
                                  .withOrder(1)
                                  .build())
            .withComplexField(newCaseField()
                                  .withId("complex2")
                                  .withFieldType(aFieldType()
                                                     .withType(COLLECTION)
                                                     .withCollectionFieldType(aFieldType()
                                                                                  .withType(COMPLEX)
                                                                                  .withComplexField(newCaseField()
                                                                                                        .withId("complex3")
                                                                                                        .withFieldType(complexOfSimpleTypes)
                                                                                                        .withOrder(1)
                                                                                                        .build())
                                                                                  .withComplexField(simpleField("simple14", 4))
                                                                                  .withComplexField(simpleField("simple13", 5))
                                                                                  .build())
                                                     .build())
                                  .withOrder(2)
                                  .build())
            .withComplexField(newCaseField()
                                  .withId("complex4")
                                  .withFieldType(aFieldType()
                                                     .withType(COMPLEX)
                                                     .withComplexField(simpleField("simple11", 1))
                                                     .withComplexField(simpleField("simple12", 2))
                                                     .build())
                                  .withOrder(3)
                                  .build())
            .withComplexField(simpleField("simple16", 4))
            .withComplexField(simpleField("simple15", 5))
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(multipleNestedCompoundFieldType)
                                    .build());

        List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(
            builder().reference("complex2.complex3.simple1").order(3).build(),
            builder().reference("complex2.complex3.simple2").order(2).build(),
            builder().reference("complex2.complex3.simple3").order(1).build(),
            builder().reference("complex2.complex3.simple5").order(4).build(),
            builder().reference("complex2.simple14").order(8).build(),
            builder().reference("complex2.simple13").order(6).build(),
            builder().reference("complex1.simple10").order(4).build(),
            builder().reference("complex1.simple8").order(2).build(),
            builder().reference("complex1.simple7").order(1).build(),
            builder().reference("simple16").order(9).build(),
            builder().reference("simple15").order(7).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, caseEventComplexFields, ROOT);

        List<CaseField> complexChildren = CASE_FIELD.getFieldType().getChildren();
        List<CaseField> complex1Children = complexChildren.get(2).getFieldType().getChildren();
        assertThat(complexChildren, contains(allOf(hasProperty("id", is("simple15")),
                                                   hasProperty("order", is(5))),
                                             allOf(hasProperty("id", is("simple16")),
                                                   hasProperty("order", is(4))),
                                             allOf(hasProperty("id", is("complex1")),
                                                   hasProperty("order", is(1))),
                                             allOf(hasProperty("id", is("complex2")),
                                                   hasProperty("order", is(2))),
                                             allOf(hasProperty("id", is("complex4")),
                                                   hasProperty("order", is(3)))));
        assertThat(complex1Children, contains(allOf(hasProperty("id", is("simple7")),
                                                    hasProperty("order", is(3))),
                                              allOf(hasProperty("id", is("simple8")),
                                                    hasProperty("order", is(2))),
                                              allOf(hasProperty("id", is("simple10")),
                                                    hasProperty("order", is(5))),
                                              allOf(hasProperty("id", is("simple9")),
                                                    hasProperty("order", is(1))),
                                              allOf(hasProperty("id", is("simple6")),
                                                    hasProperty("order", is(4)))));
        List<CaseField> complex2Children = complexChildren.get(3).getFieldType().getChildren();
        assertThat(complex2Children, contains(allOf(hasProperty("id", is("simple13")),
                                                   hasProperty("order", is(5))),
                                             allOf(hasProperty("id", is("simple14")),
                                                   hasProperty("order", is(4))),
                                             allOf(hasProperty("id", is("complex3")),
                                                   hasProperty("order", is(1)))));
        List<CaseField> complex3Children = complex2Children.get(2).getFieldType().getChildren();
        assertThat(complex3Children, contains(allOf(hasProperty("id", is("simple3")),
                                                    hasProperty("order", is(5))),
                                              allOf(hasProperty("id", is("simple2")),
                                                    hasProperty("order", is(4))),
                                              allOf(hasProperty("id", is("simple1")),
                                                    hasProperty("order", is(3))),
                                              allOf(hasProperty("id", is("simple5")),
                                                    hasProperty("order", is(2))),
                                              allOf(hasProperty("id", is("simple4")),
                                                    hasProperty("order", is(1)))));
        List<CaseField> complex4Children = complexChildren.get(4).getFieldType().getChildren();
        assertThat(complex4Children, contains(allOf(hasProperty("id", is("simple11")),
                                                    hasProperty("order", is(1))),
                                              allOf(hasProperty("id", is("simple12")),
                                                    hasProperty("order", is(2)))));

    }

    @Test
    @DisplayName("should override deeper level case fields order only if at least one case event fields exist")
    void shouldOverrideDeeperLevelCaseFieldsOrderIfCaseEventFieldsOrderExists() {
        FieldType complexOfSimpleTypes = aFieldType()
            .withType(COMPLEX)
            .withComplexField(simpleField("simple4", 1))
            .withComplexField(simpleField("simple5", 2))
            .withComplexField(simpleField("simple1", 3))
            .withComplexField(simpleField("simple2", 4))
            .withComplexField(simpleField("simple3", 5))
            .build();
        FieldType multipleNestedCompoundFieldType = aFieldType()
            .withType(COMPLEX)
            .withComplexField(newCaseField()
                                  .withId("complex1")
                                  .withFieldType(aFieldType()
                                                     .withType(COMPLEX)
                                                     .withComplexField(simpleField("simple9", 1))
                                                     .withComplexField(simpleField("simple8", 2))
                                                     .withComplexField(simpleField("simple7", 3))
                                                     .withComplexField(simpleField("simple6", 4))
                                                     .withComplexField(simpleField("simple10", 5))
                                                     .build())
                                  .withOrder(1)
                                  .build())
            .withComplexField(newCaseField()
                                  .withId("complex2")
                                  .withFieldType(aFieldType()
                                                     .withType(COLLECTION)
                                                     .withCollectionFieldType(aFieldType()
                                                                                  .withType(COMPLEX)
                                                                                  .withComplexField(newCaseField()
                                                                                                        .withId("complex3")
                                                                                                        .withFieldType(complexOfSimpleTypes)
                                                                                                        .withOrder(1)
                                                                                                        .build())
                                                                                  .build())
                                                     .build())
                                  .withOrder(2)
                                  .build())
            .build();

        CASE_FIELD.setFieldType(aFieldType()
                                    .withType(COLLECTION)
                                    .withCollectionFieldType(multipleNestedCompoundFieldType)
                                    .build());

        List<CaseEventFieldComplex> caseEventComplexFields = Lists.newArrayList(
            builder().reference("complex2.complex3.simple3").order(3).build(),
            builder().reference("complex1simple9").order(2).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(CASE_FIELD, caseEventComplexFields, ROOT);

        List<CaseField> complexChildren = CASE_FIELD.getFieldType().getChildren();
        List<CaseField> complex1Children = complexChildren.get(0).getFieldType().getChildren();
        assertThat(complex1Children, contains(allOf(hasProperty("id", is("simple9")),
                                                    hasProperty("order", is(1))),
                                              allOf(hasProperty("id", is("simple8")),
                                                    hasProperty("order", is(2))),
                                              allOf(hasProperty("id", is("simple7")),
                                                    hasProperty("order", is(3))),
                                              allOf(hasProperty("id", is("simple6")),
                                                    hasProperty("order", is(4))),
                                              allOf(hasProperty("id", is("simple10")),
                                                    hasProperty("order", is(5)))));
        List<CaseField> complex2Children = complexChildren.get(1).getFieldType().getChildren();
        List<CaseField> complex3Children = complex2Children.get(0).getFieldType().getChildren();
        assertThat(complex3Children, contains(allOf(hasProperty("id", is("simple3")),
                                                    hasProperty("order", is(5))),
                                              allOf(hasProperty("id", is("simple4")),
                                                    hasProperty("order", is(1))),
                                              allOf(hasProperty("id", is("simple5")),
                                                    hasProperty("order", is(2))),
                                              allOf(hasProperty("id", is("simple1")),
                                                    hasProperty("order", is(3))),
                                              allOf(hasProperty("id", is("simple2")),
                                                    hasProperty("order", is(4)))));
    }

    private CaseField simpleField(final String id, final Integer order) {
        return newCaseField()
            .withId(id)
            .withFieldType(simpleType())
            .withOrder(order)
            .build();
    }

    private FieldType simpleType() {
        return aFieldType().withType(TEXT_TYPE).build();
    }

}
