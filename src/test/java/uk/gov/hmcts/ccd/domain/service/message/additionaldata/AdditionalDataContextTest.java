package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;

class AdditionalDataContextTest {

    @Mock
    private CaseDetails caseDetails;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;

    private static final String FIELD_ID_1 = "FieldId1";
    private static final String FIELD_ID_2 = "FieldId2";
    private static final String FIELD_ALIAS = "FieldAlias";
    private static final String NESTED_FIELD_1 = "NestedField1";
    private static final String NESTED_FIELD_2 = "NestedField2";
    private static final String SUB_NESTED_FIELD_1 = "SubNestedField1";
    private static final String SUB_NESTED_FIELD_2 = "SubNestedField2";
    private static final String COMPLEX_ID_1 = "SomeComplexType";
    private static final String COMPLEX_ID_2 = "AnotherComplexType";
    private static final String FIELD_SEPARATOR = ".";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldGetPublishableFieldsOnly() {
        caseEventDefinition = CaseEventDefinition.builder()
            .caseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID_1)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("NotToBePublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID_2)
                    .withDisplayContext(DisplayContext.OPTIONAL)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = CaseTypeDefinition.builder()
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder()
                    .id(FIELD_ID_1)
                    .fieldTypeDefinition(textField())
                    .build(),
                CaseFieldDefinition.builder()
                    .id("NotToBePublished")
                    .fieldTypeDefinition(textField())
                    .build(),
                CaseFieldDefinition.builder()
                    .id(FIELD_ID_2)
                    .fieldTypeDefinition(textField())
                    .build()
                ))
            .build();


        AdditionalDataContext result = new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        PublishableField publishableField1 = result.getPublishableFields().get(0);
        PublishableField publishableField2 = result.getPublishableFields().get(1);

        assertAll(
            () -> assertThat(result.getPublishableFields().size(), is(2)),
            () -> assertThat(publishableField1.getKey(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getPath(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getOriginalId(), is(FIELD_ID_1)),
            () -> assertThat(publishableField2.getKey(), is(FIELD_ID_2)),
            () -> assertThat(publishableField2.getPath(), is(FIELD_ID_2)),
            () -> assertThat(publishableField2.getOriginalId(), is(FIELD_ID_2)),
            () -> assertThat(result.getTopLevelPublishables().size(), is(2)),
            () -> assertThat(result.getTopLevelPublishables().get(0), is(publishableField1)),
            () -> assertThat(result.getTopLevelPublishables().get(1), is(publishableField2)),
            () -> assertThat(result.getNestedPublishables().isEmpty(), is(true))
        );
    }

    @Test
    void shouldGetPublishableFieldsForComplexWithoutOverrides() {
        caseEventDefinition = CaseEventDefinition.builder()
            .caseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID_1)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = CaseTypeDefinition.builder()
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder()
                    .id(FIELD_ID_1)
                    .fieldTypeDefinition(
                        FieldTypeDefinition.builder()
                            .id(COMPLEX_ID_1)
                            .type(COMPLEX)
                            .complexFields(List.of(complexField(NESTED_FIELD_1)))
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext result =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        PublishableField publishableField1 = result.getPublishableFields().get(0);

        assertAll(
            () -> assertThat(result.getPublishableFields().size(), is(1)),
            () -> assertThat(publishableField1.getDisplayContext(), is(DisplayContext.MANDATORY)),
            () -> assertThat(publishableField1.getKey(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getPath(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getOriginalId(), is(FIELD_ID_1)),
            () -> assertThat(result.getTopLevelPublishables().size(), is(1)),
            () -> assertThat(result.getTopLevelPublishables().get(0), is(publishableField1)),
            () -> assertThat(result.getNestedPublishables().isEmpty(), is(true))
        );
    }

    @Test
    void shouldGetPublishableFieldsForComplexWithOverrides() {
        caseEventDefinition = CaseEventDefinition.builder()
            .caseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID_1)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_1)
                            .publish(true)
                            .publishAs(FIELD_ALIAS)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference("NotToBePublished")
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = CaseTypeDefinition.builder()
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder()
                    .id(FIELD_ID_1)
                    .fieldTypeDefinition(
                        FieldTypeDefinition.builder()
                            .id(COMPLEX_ID_1)
                            .type(COMPLEX)
                            .complexFields(List.of(complexField(NESTED_FIELD_1), complexField(NESTED_FIELD_1),
                                CaseFieldDefinition.builder()
                                    .id(NESTED_FIELD_2)
                                    .fieldTypeDefinition(
                                        FieldTypeDefinition.builder()
                                            .id(COMPLEX_ID_2)
                                            .type(COMPLEX)
                                            .complexFields(List.of(
                                                complexField(SUB_NESTED_FIELD_1), complexField(SUB_NESTED_FIELD_2)))
                                            .build()
                                    )
                                    .build(),
                                complexField("NotToBePublished")))
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext result =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        PublishableField publishableField1 = result.getPublishableFields().get(0);
        PublishableField publishableField2 = result.getPublishableFields().get(1);
        PublishableField publishableField3 = result.getPublishableFields().get(2);
        PublishableField publishableField4 = result.getPublishableFields().get(3);

        assertAll(
            () -> assertThat(result.getPublishableFields().size(), is(4)),
            () -> assertThat(publishableField1.getDisplayContext(), is(DisplayContext.COMPLEX)),
            () -> assertThat(publishableField1.getKey(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getPath(), is(FIELD_ID_1)),
            () -> assertThat(publishableField1.getOriginalId(), is(FIELD_ID_1)),
            () -> assertThat(publishableField2.getKey(), is(FIELD_ALIAS)),
            () -> assertThat(publishableField2.getPath(), is(FIELD_ID_1 + FIELD_SEPARATOR +  NESTED_FIELD_1)),
            () -> assertThat(publishableField2.getOriginalId(), is(NESTED_FIELD_1)),
            () -> assertThat(publishableField3.getKey(), is(NESTED_FIELD_2)),
            () -> assertThat(publishableField3.getPath(), is(FIELD_ID_1 + FIELD_SEPARATOR + NESTED_FIELD_2)),
            () -> assertThat(publishableField3.getOriginalId(), is(NESTED_FIELD_2)),
            () -> assertThat(publishableField4.getKey(),
                is(NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)),
            () -> assertThat(publishableField4.getPath(),
                is(FIELD_ID_1 + FIELD_SEPARATOR + NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)),
            () -> assertThat(publishableField4.getOriginalId(),
                is(NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)),
            () -> assertThat(result.getTopLevelPublishables().size(), is(2)),
            () -> assertThat(result.getTopLevelPublishables().get(0), is(publishableField1)),
            () -> assertThat(result.getTopLevelPublishables().get(1), is(publishableField2)),
            () -> assertThat(result.getNestedPublishables().size(), is(3)),
            () -> assertThat(result.getNestedPublishables().get(0), is(publishableField2)),
            () -> assertThat(result.getNestedPublishables().get(1), is(publishableField3)),
            () -> assertThat(result.getNestedPublishables().get(2), is(publishableField4))
        );
    }

    @Test
    void shouldReturnEmptyListsWhenNoPublishableFields() {
        caseEventDefinition = CaseEventDefinition.builder()
            .caseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID_1)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = CaseTypeDefinition.builder()
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder()
                    .id(FIELD_ID_1)
                    .fieldTypeDefinition(textField())
                    .build()
            ))
            .build();

        AdditionalDataContext result =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        assertAll(
            () -> assertThat(result.getPublishableFields().isEmpty(), is(true)),
            () -> assertThat(result.getTopLevelPublishables().isEmpty(), is(true)),
            () -> assertThat(result.getNestedPublishables().isEmpty(), is(true))
        );
    }

    private CaseFieldDefinition complexField(String id) {
        return CaseFieldDefinition.builder()
            .id(id)
            .fieldTypeDefinition(textField())
            .build();
    }

    private FieldTypeDefinition textField() {
        return FieldTypeDefinition.builder()
            .id(TEXT)
            .type(TEXT)
            .build();
    }
}
