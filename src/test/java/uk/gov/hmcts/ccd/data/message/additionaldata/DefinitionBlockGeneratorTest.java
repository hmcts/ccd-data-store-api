package uk.gov.hmcts.ccd.data.message.additionaldata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.MessagingProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.AdditionalDataContext;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlock;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlockGenerator;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DOCUMENT;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DYNAMIC_LIST;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.NUMBER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DefinitionBlockGeneratorTest {

    @InjectMocks
    private DefinitionBlockGenerator definitionBlockGenerator;

    @Mock
    private MessagingProperties messagingProperties;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String FIELD_ID = "FieldId";
    private static final String FIELD_ALIAS = "FieldAlias";
    private static final String NESTED_FIELD_1 = "NestedField1";
    private static final String NESTED_FIELD_2 = "NestedField2";
    private static final String SUB_NESTED_FIELD_1 = "SubNestedField1";
    private static final String SUB_NESTED_FIELD_2 = "SubNestedField2";
    private static final String COMPLEX_ID_1 = "SomeComplexType";
    private static final String COMPLEX_ID_2 = "AnotherComplexType";
    private static final String SIMPLE_TEXT_TYPE = "SimpleText";
    private static final String SIMPLE_DATE_TIME_TYPE = "SimpleDateTime";
    private static final String SIMPLE_NUMBER_TYPE = "SimpleNumber";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String DOCUMENT_FILENAME = "document_filename";
    private static final String DOCUMENT_URL = "document_url";
    private static final String FIELD_SEPARATOR = ".";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        Map<String, String> mappings = newHashMap();
        mappings.put(TEXT, SIMPLE_TEXT_TYPE);
        mappings.put(DATETIME, SIMPLE_DATE_TIME_TYPE);
        mappings.put(NUMBER, SIMPLE_NUMBER_TYPE);
        mappings.put(DOCUMENT, COMPLEX);
        
        Mockito.when(messagingProperties.getTypeMappings()).thenReturn(mappings);

        caseDetails = new CaseDetails();
        caseDetails.setData(newHashMap());
    }

    @Test
    void shouldBuildDefinitionForPublishableSimpleField() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("NotToBePublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(textField())
                    .build(),
                newCaseField()
                    .withId("NotToBePublished")
                    .withFieldType(textField())
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForSimpleFieldWithAlias() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .withPublishAs(FIELD_ALIAS)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(textField())
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ALIAS).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ALIAS).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ALIAS).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForCollectionOfBasicFields() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId("CollectionField-6a1350e1-618c-4ed4-8c58-1eb6ed9ff731")
                            .withType(COLLECTION)
                            .withCollectionFieldType(fieldType(NUMBER))
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COLLECTION)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(NUMBER)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForDynamicListField() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(fieldType(DYNAMIC_LIST))
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(DYNAMIC_LIST)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForDocumentField() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(fieldType(DOCUMENT))
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(DOCUMENT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().size(), is(3)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_BINARY_URL).getOriginalId(),
                is(DOCUMENT_BINARY_URL)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_BINARY_URL).getType(),
                is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_BINARY_URL).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_BINARY_URL).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_FILENAME).getOriginalId(),
                is(DOCUMENT_FILENAME)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_FILENAME).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_FILENAME).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_FILENAME).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_URL).getOriginalId(), is(DOCUMENT_URL)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_URL).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_URL).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(DOCUMENT_URL).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForCollectionOfComplexFields() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId("CollectionField-6a1350e1-618c-4ed4-8c58-1eb6ed9ff731")
                            .withType(COLLECTION)
                            .withCollectionFieldType(
                                aFieldType()
                                    .withId(COMPLEX_ID_1)
                                    .withType(COMPLEX)
                                    .withComplexField(complexField(NESTED_FIELD_1))
                                    .withComplexField(complexField(NESTED_FIELD_2, DATETIME))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COLLECTION)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().size(), is(2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getOriginalId(),
                is(NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getOriginalId(),
                is(NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getType(),
                is(SIMPLE_DATE_TIME_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getSubtype(), is(DATETIME)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForComplexWithoutComplexOverrides() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(complexField(NESTED_FIELD_1))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().size(), is(2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getOriginalId(),
                is(NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getOriginalId(),
                is(NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getSubtype(), is(COMPLEX_ID_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef().size(), is(2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getOriginalId(), is(SUB_NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_2).getOriginalId(), is(SUB_NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_2).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_2).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_2).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForComplexWithComplexOverrides() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference("NestedField2.SubNestedField1")
                            .publish(true)
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(complexField(NESTED_FIELD_1))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getOriginalId(),
                is(NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getSubtype(), is(COMPLEX_ID_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef().size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getOriginalId(), is(SUB_NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildDefinitionForComplexWithComplexOverridesWithAlias() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2)
                            .publish(true)
                            .publishAs(FIELD_ALIAS)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(complexField(NESTED_FIELD_1))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getOriginalId(),
                is(NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getSubtype(), is(COMPLEX_ID_2)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef().size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getOriginalId(), is(SUB_NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_2).getTypeDef()
                .get(SUB_NESTED_FIELD_1).getTypeDef(), is(nullValue())),
            () -> assertThat(result.get(FIELD_ALIAS).getOriginalId(), is(NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ALIAS).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ALIAS).getSubtype(), is(COMPLEX_ID_2)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().size(), is(2)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_1).getOriginalId(),
                is(SUB_NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_1).getType(),
                is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_1).getTypeDef(),
                is(nullValue())),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_2).getOriginalId(),
                is(SUB_NESTED_FIELD_2)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_2).getType(),
                is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_2).getSubtype(),
                is(TEXT)),
            () -> assertThat(result.get(FIELD_ALIAS).getTypeDef().get(SUB_NESTED_FIELD_2).getTypeDef(),
                is(nullValue()))
        );
    }

    @Test
    void shouldNotBuildTypeDefinitionForComplexWithMissingComplexParent() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            // Definition will not be generated due to not also defining parent NestedField2
                            .reference(NESTED_FIELD_2 + FIELD_SEPARATOR + SUB_NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(complexField(NESTED_FIELD_1))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().isEmpty(), is(true))
        );
    }

    @Test
    void shouldBuildDefinitionForComplexWithOverridesButTopLevelNotPublishable() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(false)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_1)
                                    .withFieldType(textField())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);
        
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(COMPLEX)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(COMPLEX_ID_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getOriginalId(),
                is(NESTED_FIELD_1)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getType(), is(SIMPLE_TEXT_TYPE)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getSubtype(), is(TEXT)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef().get(NESTED_FIELD_1).getTypeDef(), is(nullValue()))
        );
    }

    @Test
    void shouldDefaultFieldTypeForTypesWithoutMappings() {
        String fieldType = "NewType";

        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(fieldType(fieldType))
                    .build()
            ))
            .build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, DefinitionBlock> result = definitionBlockGenerator.generateDefinition(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).getOriginalId(), is(FIELD_ID)),
            () -> assertThat(result.get(FIELD_ID).getType(), is(fieldType)),
            () -> assertThat(result.get(FIELD_ID).getSubtype(), is(fieldType)),
            () -> assertThat(result.get(FIELD_ID).getTypeDef(), is(nullValue()))
        );
    }

    private CaseFieldDefinition complexField(String id, String type) {
        return newCaseField()
            .withId(id)
            .withFieldType(fieldType(type))
            .build();
    }

    private CaseFieldDefinition complexField(String id) {
        return newCaseField()
            .withId(id)
            .withFieldType(textField())
            .build();
    }

    private FieldTypeDefinition textField() {
        return fieldType(TEXT);
    }

    private FieldTypeDefinition fieldType(String type) {
        return aFieldType()
            .withId(type)
            .withType(type)
            .build();
    }
}