package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.MONEY_GBP;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.NUMBER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.YES_OR_NO;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.message.additionaldata.PublishableField.FIELD_SEPARATOR;

class DataBlockGeneratorTest {

    @InjectMocks
    private DataBlockGenerator dataBlockGenerator;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FIELD_ID = "FieldId";
    private static final String FIELD_ALIAS = "FieldAlias";
    private static final String TEXT_FIELD = "TextValue";
    private static final String NESTED_FIELD_1 = "NestedField1";
    private static final String NESTED_FIELD_2 = "NestedField2";
    private static final String SUB_NESTED_FIELD_1 = "SubNestedField1";
    private static final String SUB_NESTED_FIELD_2 = "SubNestedField2";
    private static final String SUB_NESTED_FIELD_3 = "SubNestedField2";
    private static final String COMPLEX_ID_1 = "ComplexType1";
    private static final String COMPLEX_ID_2 = "ComplexType2";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void shouldBuildDataForPublishableSimpleBooleanField() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("dontPublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build(),
                newCaseField()
                    .withId("dontPublished")
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, new TextNode("Yes"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertTrue(result.get(FIELD_ID).booleanValue()),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleNumberField() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("dontPublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.MONEY_GBP)
                        .withType(FieldTypeDefinition.MONEY_GBP)
                        .build())
                    .build(),
                newCaseField()
                    .withId("dontPublished")
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, IntNode.valueOf(6080));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);
        assertAll(
            () -> assertEquals(6080, result.get(FIELD_ID).intValue()),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleTextFieldAlias() {
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
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.TEXT)
                        .withType(FieldTypeDefinition.TEXT)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue(TEXT_FIELD, JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.get(FIELD_ALIAS).textValue(), is("TextValue")),
            () -> assertThat(result.size(), is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleAddressUkField() throws JsonProcessingException {
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
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK)
                        .withType(COMPLEX)
                        .withComplexField(complexField("AddressLine1", TEXT))
                        .withComplexField(complexField("AddressLine2", TEXT))
                        .withComplexField(complexField("AddressLine3", TEXT))
                        .withComplexField(complexField("Country", TEXT))
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        final JsonNode DATA = mapper.readTree("{\n"
            + "    \"AddressLine1\" : \"line 1\",\n"
            + "    \"AddressLine2\" : \"line 2\",\n"
            + "    \"AddressLine3\" : \"line 3\",\n"
            + "    \"Country\" : \"country\"\n"
            + "  }");

        data.put(FIELD_ID, DATA);

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(DATA, result.get(FIELD_ALIAS)),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleCollectionField() throws JsonProcessingException {
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
                    .withFieldType(aFieldType()
                        .withId(COLLECTION)
                        .withType(COLLECTION)
                        .withCollectionFieldType(fieldType(TEXT))
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        final JsonNode DATA = mapper.readTree("[\n"
            + "    {\n"
            + "        \"id\": \"111\",\n"
            + "        \"value\": \"CollectionValue1\"\n"
            + "    },\n"
            + "    {\n"
            + "        \"id\": \"222\",\n"
            + "        \"value\": \"CollectionValue2\"\n"
            + "    },\n"
            + "    {\n"
            + "        \"id\": \"333\",\n"
            + "        \"value\": \"CollectionValue3\"\n"
            + "    }\n"
            + "]");

        data.put(FIELD_ID, DATA);

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID), is(DATA))
        );
    }

    @Test
    void shouldBuildDataForPublishableComplexCollectionField() throws JsonProcessingException {
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
                            .withId(COLLECTION)
                            .withType(COLLECTION)
                            .withCollectionFieldType(
                                aFieldType()
                                    .withId(COMPLEX_ID_2)
                                    .withType(COMPLEX)
                                    .withComplexField(complexField(SUB_NESTED_FIELD_1, YES_OR_NO))
                                    .withComplexField(complexField(SUB_NESTED_FIELD_2, NUMBER))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        JsonNode fieldData = objectMapper.readTree("[\n"
            + "        {\n"
            + "            \"id\": \"123\",\n"
            + "            \"value\": {\n"
            + "                \"SubNestedField1\": null,\n"
            + "                \"SubNestedField2\": \"1111\"\n"
            + "            }\n"
            + "        },\n"
            + "        {\n"
            + "            \"id\": \"456\",\n"
            + "            \"value\": {\n"
            + "                \"SubNestedField1\": \"Yes\",\n"
            + "                \"SubNestedField2\": null\n"
            + "            }\n"
            + "        }\n"
            + "    ]");
        data.put(FIELD_ID, fieldData);

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("[{\"id\":\"123\",\"value\""
                + ":{\"SubNestedField1\":null,\"SubNestedField2\":1111}},"
                + "{\"id\":\"456\",\"value\":{\"SubNestedField1\":true,"
                + "\"SubNestedField2\":null}}]"))
        );
    }

    @Test
    void shouldBuildDataForPublishableComplexField() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_3, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        JsonNode jsonData = mapper.readTree("{\n"
            + "      \"NestedField1\": \"valueOne\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": null\n"
            + "      }\n"
            + "  }");
        data.put(FIELD_ID, jsonData);

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID), is(jsonData))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverrides() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField1\": \"valueOne\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": \"valueThree\"\n"
            + "      }\n"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);
        ObjectNode nestedFieldTwo = mapper.valueToTree(result.get(FIELD_ID));

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(nestedFieldTwo.get(NESTED_FIELD_2).size(), is(1)),
            () -> assertThat(nestedFieldTwo.findValue(SUB_NESTED_FIELD_1).asText(), is("valueTwo"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverrides2() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField1\": \"valueOne\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": \"valueThree\"\n"
            + "      }\n"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("{\"NestedField2\":{\"SubNestedField1\":"
                + "\"valueTwo\",\"SubNestedField2\":\"valueThree\"}}"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithCollectionOverrides() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COLLECTION)
                                            .withCollectionFieldType(
                                                aFieldType()
                                                    .withId("SomeOtherType")
                                                    .withType(COMPLEX)
                                                    .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                                    .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "    \"NestedField2\": [\n"
            + "        {\n"
            + "            \"id\": \"123\",\n"
            + "            \"value\": {\n"
            + "                \"SubNestedField1\": \"CollectionValue1\",\n"
            + "                \"SubNestedField2\": \"SomethingToBeIgnored\"\n"
            + "            }\n"
            + "        },\n"
            + "        {\n"
            + "            \"id\": \"456\",\n"
            + "            \"value\": {\n"
            + "                \"SubNestedField1\": \"CollectionValue2\",\n"
            + "                \"SubNestedField2\": \"SomethingToBeIgnored\"\n"
            + "            }\n"
            + "        }\n"
            + "    ]\n"
            + "}"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("{\"NestedField2\":[{\"id\":\"123\",\"value\""
                + ":{\"SubNestedField1\":\"CollectionValue1\"}},{\"id\":\"456\",\"value\":"
                + "{\"SubNestedField1\":\"CollectionValue2\"}}]}"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithCollectionOverrides2() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COLLECTION)
                                            .withCollectionFieldType(fieldType(TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "    \"NestedField2\": [\n"
            + "        {\n"
            + "            \"id\": \"123\",\n"
            + "            \"value\": \"Value1\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"id\": \"456\",\n"
            + "            \"value\": \"Value2\"\n"
            + "        }\n"
            + "    ]\n"
            + "}"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("{\"NestedField2\":[{\"id\":\"123\","
                + "\"value\":\"Value1\"},{\"id\":\"456\",\"value\":\"Value2\"}]}"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithCollectionOverrides3() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COLLECTION)
                                            .withCollectionFieldType(fieldType(TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "    \"NestedField2\": [\n"
            + "        {\n"
            + "            \"id\": \"123\",\n"
            + "            \"value\": \"CollectionValue1\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"id\": \"456\",\n"
            + "            \"value\": \"CollectionValue2\"\n"
            + "        }\n"
            + "    ]\n"
            + "}"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("{\"NestedField2\":[{\"id\":\"123\",\"value\":"
                + "\"CollectionValue1\"},{\"id\":\"456\",\"value\":\"CollectionValue2\"}]}"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverridesWithAlias() throws JsonProcessingException {
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField1\": \"valueOne\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": \"valueThree\"\n"
            + "      }\n"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();


        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result.get(FIELD_ID).toString(),
                is("{\"NestedField2\":{\"SubNestedField1\":\"valueTwo\"}}")),
            () -> assertThat(result.get(FIELD_ALIAS).toString(),
                is("{\"SubNestedField1\":\"valueTwo\",\"SubNestedField2\":\"valueThree\"}"))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverridesWithMoneyGBP() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference("NestedField2.SubNestedField2")
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
                            .withComplexField(complexField(NESTED_FIELD_1, MONEY_GBP))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField1\": \"1271\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": \"valueThree\"\n"
            + "      }\n"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();


        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);
        ObjectNode nestedField = mapper.valueToTree(result.get(FIELD_ID));

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(nestedField.findValue(NESTED_FIELD_1).intValue(), is(1271))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverridesWithYesOrNoField() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_1)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference("NestedField2.SubNestedField2")
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
                            .withComplexField(complexField(NESTED_FIELD_1, YES_OR_NO))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField1\": \"No\",\n"
            + "      \"NestedField2\": {\n"
            + "        \"SubNestedField1\": \"valueTwo\",\n"
            + "        \"SubNestedField2\": \"valueThree\"\n"
            + "      }\n"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();


        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);
        ObjectNode nestedField = mapper.valueToTree(result.get(FIELD_ID));

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(nestedField.findValue(NESTED_FIELD_1).asBoolean(), is(false))
        );
    }

    @Test
    void shouldBuildDataForPublishableFieldWithNullValue() {
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
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.TEXT)
                        .withType(FieldTypeDefinition.TEXT)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, NullNode.getInstance());

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).isNull(), is(true))
        );
    }

    @Test
    void shouldBuildDataForPublishableFieldWithNoKey() {
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
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.TEXT)
                        .withType(FieldTypeDefinition.TEXT)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).isNull(), is(true))
        );
    }

    @Test
    void shouldBuildDataForPublishableBooleanFieldWithNullValue() {
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
                    .withFieldType(aFieldType()
                        .withId(YES_OR_NO)
                        .withType(YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, NullNode.getInstance());

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).isNull(), is(true))
        );
    }

    @Test
    void shouldBuildDataForPublishableBooleanFieldWithEmptyValue() {
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
                    .withFieldType(aFieldType()
                        .withId(YES_OR_NO)
                        .withType(YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, new TextNode(""));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).isNull(), is(true))
        );
    }

    @Test
    void shouldBuildDataForPublishableNumberFieldWithNullValue() {
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
                    .withFieldType(aFieldType()
                        .withId(NUMBER)
                        .withType(NUMBER)
                        .build())
                    .build()
            ))
            .build();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, NullNode.getInstance());

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).isNull(), is(true))
        );
    }

    @Test
    void shouldBuildDataForPublishableComplexOverrideFieldWithNullValue() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_1)
                            .publish(true)
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
                            .withComplexField(complexField(NESTED_FIELD_1, TEXT))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1, TEXT))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2, TEXT))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.readTree("{\n"
            + "      \"NestedField2\": null"
            + "  }"));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, JsonNode> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(FIELD_ID).toString(), is("{\"NestedField1\":null,\"NestedField2\":null}"))
        );
    }

    private CaseFieldDefinition complexField(String id, String type) {
        return newCaseField()
            .withId(id)
            .withFieldType(fieldType(type))
            .build();
    }

    private FieldTypeDefinition fieldType(String type) {
        return aFieldType()
            .withId(type)
            .withType(type)
            .build();
    }
}
