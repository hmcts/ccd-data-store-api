package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
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
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DataBlockGeneratorTest {

    @InjectMocks
    private DataBlockGenerator dataBlockGenerator;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String FIELD_ID = "FieldId";
    private static final String FIELD_ALIAS = "FieldAlias";
    private static final String TEXT_FIELD = "TextValue";
    private static final String NESTED_FIELD_1 = "NestedField1";
    private static final String NESTED_FIELD_2 = "NestedField2";
    private static final String SUB_NESTED_FIELD_1 = "SubNestedField1";
    private static final String SUB_NESTED_FIELD_2 = "SubNestedField2";
    private static final String COMPLEX_ID_1 = "ComplexType1";
    private static final String COMPLEX_ID_2 = "ComplexType2";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Ignore
    @Test
    void generateDefinitionForSimpleDataFields() throws JsonProcessingException {
        AdditionalDataContext context = new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        String stringResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        System.out.println(stringResult);

        assertEquals(result.toString(), "{AddressField={\"AddressLine1\":\"lin 1\",\"AddressLine2\":\"line 2\",\"AddressLine3\":\"line 3\",\"Country\":\"country\"}, AddressUKField={\"AddressLine1\":null,\"AddressLine2\":null,\"AddressLine3\":null,\"PostTown\":null,\"County\":null,\"PostCode\":null,\"Country\":null}, MoneyField=60000, DocumentField1=null, MarritalStatus=\"CIVIL_PARTNERSHIP\", NumberField=66, MultiSelectField=[\"MANCHESTER\",\"CARDIFF\"], EmailField=\"test@test.com\", YesNoField=false, PhoneField=\"07971238417\", TextField=\"Text field\", DateField=\"2000-01-01\", CollectionFieldMan=[], TextAreaField=\"text area\", CollectionField=[{\"value\":\"collextion field\",\"id\":\"d06f0976-4183-403d-955e-c9b54aad3bc0\"},{\"value\":\"collection field two\",\"id\":\"f2ab95f3-0eb9-43dc-8f5b-b9524ac8e518\"}]}");
    }

    @Test
    void shouldBuildDataForPublishableSimpleBooleanField() throws JsonProcessingException {
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

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("No", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ID), false),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleNumberField() throws JsonProcessingException {
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
        data.put(FIELD_ID, mapper.convertValue("60", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ID), 60),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleTextFieldAlias() throws JsonProcessingException {
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

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ALIAS), "TextValue"),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
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
                        .withType(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("\"AddressField\" : {\n" +
            "    \"AddressLine1\" : \"lin 1\",\n" +
            "    \"AddressLine2\" : \"line 2\",\n" +
            "    \"AddressLine3\" : \"line 3\",\n" +
            "    \"Country\" : \"country\"\n" +
            "  }", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ALIAS), "\"AddressField\" : {\n" +
                "    \"AddressLine1\" : \"lin 1\",\n" +
                "    \"AddressLine2\" : \"line 2\",\n" +
                "    \"AddressLine3\" : \"line 3\",\n" +
                "    \"Country\" : \"country\"\n" +
                "  }"),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Ignore
    @Test
    void shouldBuildDataForComplexWithComplexOverrides() {
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

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("{\"FieldId\":{\"ComplexType1\":{\"NestedField1\":\"valueOne\","
            + "\"NestedField2\":{\"SubNestedField1\":\"valueTwo\",\"SubNestedField2\":\"valueThree\"}}}}", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();


        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(NESTED_FIELD_2), is(1))
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
