package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeEntryProcessor;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;

class DateTimeEntryProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ID = "FieldId";

    private static final String DATETIME_FIELD_TYPE = "DateTime";
    private static final String DATE_FIELD_TYPE = "Date";
    private static final String COLLECTION_FIELD_TYPE = "Collection";
    private static final String COMPLEX_FIELD_TYPE = "Complex";

    @InjectMocks
    private DateTimeEntryProcessor dateTimeEntryProcessor;

    @Mock
    private DateTimeFormatParser dateTimeFormatParser;

    @Mock
    private CaseViewFieldBuilder caseViewFieldBuilder;

    @Mock
    private FieldTypeDefinition fieldType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.EMPTY_LIST);
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();
        setUpBaseTypes();
    }

    @Test
    void shouldReturnProcessedNodeForSimpleField() throws IOException {
        String json = "{\"DateTimeField\":\"13/03/2020\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        when(caseViewFieldBuilder.build(any(), any())).thenReturn(caseViewField);
        when(dateTimeFormatParser.valueToTextNode(eq("13/03/2020"), eq(BaseType.get(DATETIME)), any(), eq("dd/MM/yyyy"), eq(true)))
            .thenReturn(new TextNode("2020-03-13T00:00:00.000"));

        JsonNode result = dateTimeEntryProcessor.execute(node,
            new CaseFieldDefinition(),
            new CaseEventFieldDefinition(),
            wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForSimpleFieldNullDisplayContextParameter() throws IOException {
        String json = "{\"DateTimeField\":\"2020-03-13T00:00:00.000\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, null, fieldType());
        when(caseViewFieldBuilder.build(any(), any())).thenReturn(caseViewField);
        when(dateTimeFormatParser.valueToTextNode(eq("2020-03-13T00:00:00.000"), eq(BaseType.get(DATETIME)), any(), eq(null), eq(false)))
            .thenReturn(new TextNode("2020-03-13T00:00:00.000"));

        JsonNode result = dateTimeEntryProcessor.execute(node,
            new CaseFieldDefinition(),
            new CaseEventFieldDefinition(),
            wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForSimpleFieldIncorrectDisplayContextParameter() throws IOException {
        String json = "{\"DateTimeField\":\"2020-03-13T00:00:00.000\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        when(caseViewFieldBuilder.build(any(), any())).thenReturn(caseViewField);
        when(dateTimeFormatParser.valueToTextNode(eq("2020-03-13T00:00:00.000"), eq(BaseType.get(DATETIME)), any(), eq("dd/MM/yyyy"), eq(true)))
            .thenReturn(new TextNode("2020-03-13T00:00:00.000"));

        JsonNode result = dateTimeEntryProcessor.execute(node,
            new CaseFieldDefinition(),
            new CaseEventFieldDefinition(),
            wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForCollectionField() throws IOException {
        String json = "{\"CollectionField\":[{\"id\":\"id1\",\"value\":\"13/03/2020\"},{\"id\":\"id2\",\"value\":\"25/12/1995\"}]}";
        JsonNode node = MAPPER.readTree(json).get("CollectionField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)",
            fieldType("Collection", "Collection", Collections.EMPTY_LIST, fieldType(
                "DateTime", "DateTime", Collections.EMPTY_LIST, null
            ))
        );
        when(caseViewFieldBuilder.build(any(), any())).thenReturn(caseViewField);
        when(dateTimeFormatParser.valueToTextNode(eq("13/03/2020"), eq(BaseType.get(DATETIME)), any(), eq("dd/MM/yyyy"), eq(true)))
            .thenReturn(new TextNode("2020-03-13T00:00:00.000"));
        when(dateTimeFormatParser.valueToTextNode(eq("25/12/1995"), eq(BaseType.get(DATETIME)), any(), eq("dd/MM/yyyy"), eq(true)))
            .thenReturn(new TextNode("1995-12-25T00:00:00.000"));

        JsonNode result = dateTimeEntryProcessor.execute(node,
            new CaseFieldDefinition(),
            new CaseEventFieldDefinition(),
            wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isArray(), is(true)),
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result.get(0).get(CollectionValidator.VALUE).asText(), is("2020-03-13T00:00:00.000")),
            () -> assertThat(result.get(0).get("id").asText(), is("id1")),
            () -> assertThat(result.get(1).get(CollectionValidator.VALUE).asText(), is("1995-12-25T00:00:00.000")),
            () -> assertThat(result.get(1).get("id").asText(), is("id2"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForComplexField() throws IOException {
        String json = "{\n"
                      + "    \"ComplexField\": {\n"
                      + "        \"ComplexDateTimeField\": \"2001\",\n"
                      + "        \"ComplexNestedField\": {\n"
                      + "            \"NestedDateField\": \"12\",\n"
                      + "            \"NestedCollectionTextField\": []\n"
                      + "        }\n"
                      + "    }\n"
                      + "}";
        JsonNode node = MAPPER.readTree(json).get("ComplexField");
        CaseFieldDefinition caseField1 = newCaseField().withId("ComplexDateTimeField")
            .withFieldType(fieldType()).withDisplayContextParameter("#DATETIMEENTRY(yyyy)").build();
        CaseFieldDefinition caseField2 = newCaseField().withId("NestedDateField")
            .withFieldType(fieldType("Date", "Date", null, null)).withDisplayContextParameter("#DATETIMEENTRY(MM)").build();
        CaseFieldDefinition caseField3 = newCaseField().withId("NestedCollectionTextField")
            .withFieldType(fieldType("Collection", "Collection", null, fieldType())).build();
        CaseFieldDefinition caseField4 = newCaseField().withId("LabelField")
            .withFieldType(fieldType("Label", "Label", null, null)).build();
        CaseFieldDefinition caseField5 = newCaseField().withId("ComplexNestedField")
            .withFieldType(fieldType("Complex", "Complex", Arrays.asList(caseField2, caseField3, caseField4), null)).build();
        CaseFieldDefinition caseField6 = newCaseField().withId("ComplexField")
            .withFieldType(fieldType("Complex", "Complex", Arrays.asList(caseField1, caseField5), null)).build();
        CaseViewField caseViewField = caseViewField(ID, null,
            fieldType("Complex", "Complex", Arrays.asList(caseField1, caseField5), null)
        );

        when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        when(dateTimeFormatParser.valueToTextNode(eq("2001"), eq(BaseType.get(DATETIME)), any(), eq("yyyy"), eq(true)))
            .thenReturn(new TextNode("2001-01-01T00:00:00.000"));
        when(dateTimeFormatParser.valueToTextNode(eq("12"), eq(BaseType.get(DATE)), any(), eq("MM"), eq(true)))
            .thenReturn(new TextNode("1970-12-01"));

        JsonNode result = dateTimeEntryProcessor.execute(node, caseField6, new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isObject(), is(true)),
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result.get("ComplexDateTimeField").asText(), is("2001-01-01T00:00:00.000")),
            () -> assertThat(result.get("ComplexNestedField").get("NestedDateField").asText(), is("1970-12-01")),
            () -> assertThat(result.get("ComplexNestedField").get("NestedCollectionTextField").isArray(), is(true)),
            () -> assertThat(result.get("ComplexNestedField").get("NestedCollectionTextField").size(), is(0))
        );
    }

    private CaseFieldDefinition caseField(String id) {
        CaseFieldDefinition caseField = new CaseFieldDefinition();
        caseField.setId(id);
        return caseField;
    }

    private WizardPageField wizardPageField(String id, List<WizardPageComplexFieldOverride> overrides) {
        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId(id);
        wizardPageField.setComplexFieldOverrides(overrides);
        return wizardPageField;
    }

    private CaseViewField caseViewField(String id, String displayContextParameter, FieldTypeDefinition fieldType) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(id);
        caseViewField.setDisplayContextParameter(displayContextParameter);
        caseViewField.setFieldTypeDefinition(fieldType);
        return caseViewField;
    }

    private FieldTypeDefinition fieldType(String id, String type, List<CaseFieldDefinition> complexFields, FieldTypeDefinition collectionFieldType) {
        FieldTypeDefinition fieldType = new FieldTypeDefinition();
        fieldType.setId(id);
        fieldType.setType(type);
        fieldType.setComplexFields(complexFields);
        fieldType.setCollectionFieldTypeDefinition(collectionFieldType);
        return fieldType;
    }

    private FieldTypeDefinition fieldType() {
        return fieldType("DateTime");
    }

    private FieldTypeDefinition fieldType(String fieldType) {
        return fieldType(fieldType, fieldType, Collections.emptyList(), null);
    }

    private void setUpBaseTypes() {
        BaseType.register(new BaseType(fieldType(DATE_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(DATETIME_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COLLECTION_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COMPLEX_FIELD_TYPE)));
    }
}
