package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService.ROOT;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition.builder;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

class CompoundFieldOrderServiceTest {

    private static final FieldTypeDefinition TEXT_FIELD_TYPE_DEFINITION =
        FieldTypeDefinition.builder().id("Text").type("Text").build();

    private CaseFieldDefinition.CaseFieldDefinitionBuilder caseField() {
        return CaseFieldDefinition.builder()
            .fieldTypeDefinition(TEXT_FIELD_TYPE_DEFINITION)
            .id("PersonFirstName");
    }

    private static final String TEXT_TYPE = "Text";
    public static final ArrayList<CaseEventFieldComplexDefinition> EMPTY_CASE_EVENT_COMPLEX_FIELDS =
        Lists.newArrayList();

    private CompoundFieldOrderService compoundFieldOrderService = new CompoundFieldOrderService();

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields null")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsNull() {
        FieldTypeDefinition collectionComplexFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(simpleField("Three", 1), simpleField("One", 2), simpleField("Two", 3)))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(collectionComplexFieldTypeDefinition)
            .build()).build();

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, null,
            ROOT);

        List<CaseFieldDefinition> complexFields = caseField.getFieldTypeDefinition().getChildren();
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
        FieldTypeDefinition collectionComplexFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(
                List.of(
                    simpleField("Three", 1), simpleField("One", 2), simpleField("Two", 3))
            )
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(collectionComplexFieldTypeDefinition)
            .build()).build();

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField,
            EMPTY_CASE_EVENT_COMPLEX_FIELDS, ROOT);

        List<CaseFieldDefinition> complexFields = caseField.getFieldTypeDefinition().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields not matching on"
        + " simple type")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsNotMatching() {
        FieldTypeDefinition collectionComplexFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(
                simpleField("Three", 1), simpleField("One", 2), simpleField("Two", 3)))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(collectionComplexFieldTypeDefinition)
            .build()).build();

        List<CaseEventFieldComplexDefinition> caseEventComplexFields =
            Lists.newArrayList(builder().reference("OneTwo").order(3).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, caseEventComplexFields, ROOT);

        List<CaseFieldDefinition> complexFields = caseField.getFieldTypeDefinition().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should leave the case field order to remain as is if case event to complex fields matching but"
        + " no order")
    void shouldLeaveTheCaseFieldOrderToRemainAsIsIfCaseEventComplexFieldsMatchingButNoOrder() {
        FieldTypeDefinition collectionComplexFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(
                simpleField("Three", 1), simpleField("One", 2), simpleField("Two", 3)))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(collectionComplexFieldTypeDefinition)
            .build()).build();

        List<CaseEventFieldComplexDefinition> caseEventComplexFields =
            Lists.newArrayList(builder().reference("One").order(null).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, caseEventComplexFields, ROOT);

        List<CaseFieldDefinition> complexFields = caseField.getFieldTypeDefinition().getChildren();
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(1))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(3)))));
    }

    @Test
    @DisplayName("should override top level case fields order from case event complex fields order even if duplicates"
        + " exist")
    void shouldOverrideTopLevelCaseFieldsOrderFromCaseEventComplexFieldsOrderEvenIfDuplicatesExist() {
        FieldTypeDefinition collectionComplexFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(
                simpleField("One", 1), simpleField("Two", 2), simpleField("Three", 3)))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(collectionComplexFieldTypeDefinition)
            .build()).build();

        List<CaseEventFieldComplexDefinition> caseEventComplexFields =
            Lists.newArrayList(builder().reference("One").order(5).build(),
                                builder().reference("One").order(6).build(),
                                builder().reference("Two").order(3).build(),
                                builder().reference("Two").order(4).build(),
                                builder().reference("Three").order(1).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, caseEventComplexFields, ROOT);

        List<CaseFieldDefinition> complexFields = caseField.getFieldTypeDefinition().getChildren();
        assertThat(complexFields, is(hasSize(3)));
        assertThat(complexFields, contains(allOf(hasProperty("id", is("Three")),
                                                 hasProperty("order", is(3))),
                                           allOf(hasProperty("id", is("Two")),
                                                 hasProperty("order", is(2))),
                                           allOf(hasProperty("id", is("One")),
                                                 hasProperty("order", is(1)))));
    }

    @Test
    @DisplayName("should override deeper level case fields order from case event fields order with missing order "
        + "values")
    void shouldOverrideDeeperLevelCaseFieldsOrderFromCaseEventFieldsOrder() {
        FieldTypeDefinition complexOfSimpleTypes = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(
                    simpleField("simple4", 1),
                    simpleField("simple5", 2),
                    simpleField("simple1", 3),
                    simpleField("simple2", 4),
                    simpleField("simple3", 5)
                )
            )
            .build();

        FieldTypeDefinition multipleNestedCompoundFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(CaseFieldDefinition.builder()
                    .id("complex1")
                    .fieldTypeDefinition(FieldTypeDefinition.builder()
                        .type(COMPLEX)
                        .complexFields(List.of(
                            simpleField("simple9", 1),
                            simpleField("simple8", 2),
                            simpleField("simple7", 3),
                            simpleField("simple6", 4),
                            simpleField("simple10", 5)
                        ))
                        .build())
                    .order(1)
                    .build(),
                CaseFieldDefinition.builder()
                    .id("complex2")
                    .fieldTypeDefinition(FieldTypeDefinition.builder()
                        .type(COLLECTION)
                        .collectionFieldTypeDefinition(FieldTypeDefinition.builder()
                            .type(COMPLEX)
                            .complexFields(
                                List.of(
                                    CaseFieldDefinition.builder().id("complex3")
                                        .fieldTypeDefinition(
                                            complexOfSimpleTypes)
                                        .order(1)
                                        .build(),
                                    simpleField("simple14", 4),
                                    simpleField("simple13", 5)
                                )
                            )
                            .build())
                        .build())
                    .order(2)
                    .build(),
                CaseFieldDefinition.builder()
                    .id("complex4")
                    .fieldTypeDefinition(FieldTypeDefinition.builder()
                        .type(COMPLEX)
                        .complexFields(List.of(simpleField("simple11", 1), simpleField("simple12", 2)))
                        .build())
                    .order(3)
                    .build(),
                simpleField("simple16", 4),
                simpleField("simple15", 5)
            ))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(multipleNestedCompoundFieldTypeDefinition)
            .build()).build();

        List<CaseEventFieldComplexDefinition> caseEventComplexFields = Lists.newArrayList(
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

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, caseEventComplexFields, ROOT);

        List<CaseFieldDefinition> complexChildren = caseField.getFieldTypeDefinition().getChildren();
        List<CaseFieldDefinition> complex1Children = complexChildren.get(2).getFieldTypeDefinition().getChildren();
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
        List<CaseFieldDefinition> complex2Children = complexChildren.get(3).getFieldTypeDefinition().getChildren();
        assertThat(complex2Children, contains(allOf(hasProperty("id", is("simple13")),
                hasProperty("order", is(5))),
            allOf(hasProperty("id", is("simple14")),
                hasProperty("order", is(4))),
            allOf(hasProperty("id", is("complex3")),
                hasProperty("order", is(1)))));
        List<CaseFieldDefinition> complex3Children = complex2Children.get(2).getFieldTypeDefinition().getChildren();
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
        List<CaseFieldDefinition> complex4Children = complexChildren.get(4).getFieldTypeDefinition().getChildren();
        assertThat(complex4Children, contains(allOf(hasProperty("id", is("simple11")),
                hasProperty("order", is(1))),
            allOf(hasProperty("id", is("simple12")),
                hasProperty("order", is(2)))));

    }

    @Test
    @DisplayName("should override deeper level case fields order only if at least one case event fields exist")
    void shouldOverrideDeeperLevelCaseFieldsOrderIfCaseEventFieldsOrderExists() {
        FieldTypeDefinition complexOfSimpleTypes = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(
                simpleField("simple4", 1),
                simpleField("simple5", 2),
                simpleField("simple1", 3),
                simpleField("simple2", 4),
                simpleField("simple3", 5)
            ))
            .build();
        FieldTypeDefinition multipleNestedCompoundFieldTypeDefinition = FieldTypeDefinition.builder()
            .type(COMPLEX)
            .complexFields(List.of(CaseFieldDefinition.builder()
                    .id("complex1")
                    .fieldTypeDefinition(FieldTypeDefinition.builder()
                        .type(COMPLEX)
                        .complexFields(List.of(
                            simpleField("simple9", 1),
                            simpleField("simple8", 2),
                            simpleField("simple7", 3),
                            simpleField("simple6", 4),
                            simpleField("simple10", 5)
                        ))
                        .build())
                    .order(1)
                    .build(),
                CaseFieldDefinition.builder()
                    .id("complex2")
                    .fieldTypeDefinition(FieldTypeDefinition.builder()
                        .type(COLLECTION)
                        .collectionFieldTypeDefinition(FieldTypeDefinition.builder()
                            .type(COMPLEX)
                            .complexFields(List.of(
                                CaseFieldDefinition.builder()
                                    .id("complex3")
                                    .fieldTypeDefinition(
                                        complexOfSimpleTypes)
                                    .order(1)
                                    .build()))
                            .build())
                        .build())
                    .order(2)
                    .build()))
            .build();

        CaseFieldDefinition caseField = caseField().fieldTypeDefinition(FieldTypeDefinition.builder()
            .type(COLLECTION)
            .collectionFieldTypeDefinition(multipleNestedCompoundFieldTypeDefinition)
            .build()).build();

        List<CaseEventFieldComplexDefinition> caseEventComplexFields = Lists.newArrayList(
            builder().reference("complex2.complex3.simple3").order(3).build(),
            builder().reference("complex1simple9").order(2).build());

        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, caseEventComplexFields, ROOT);

        List<CaseFieldDefinition> complexChildren = caseField.getFieldTypeDefinition().getChildren();
        List<CaseFieldDefinition> complex1Children = complexChildren.get(0).getFieldTypeDefinition().getChildren();
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
        List<CaseFieldDefinition> complex2Children = complexChildren.get(1).getFieldTypeDefinition().getChildren();
        List<CaseFieldDefinition> complex3Children = complex2Children.get(0).getFieldTypeDefinition().getChildren();
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

    private CaseFieldDefinition simpleField(final String id, final Integer order) {
        return CaseFieldDefinition.builder()
            .id(id)
            .fieldTypeDefinition(simpleType())
            .order(order)
            .build();
    }

    private FieldTypeDefinition simpleType() {
        return FieldTypeDefinition.builder().type(TEXT_TYPE).build();
    }

}
