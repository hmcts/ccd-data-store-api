package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
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
    private FieldType fieldType;

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
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "13/03/2020")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601(null, "2020-03-13T00:00:00.000")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "2020-03-13T00:00:00.000")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "13/03/2020")).thenReturn("2020-03-13T00:00:00.000");
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "25/12/1995")).thenReturn("1995-12-25T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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
        CaseField caseField1 = newCaseField().withId("ComplexDateTimeField")
            .withFieldType(fieldType()).withDisplayContextParameter("#DATETIMEENTRY(yyyy)").build();
        CaseField caseField2 = newCaseField().withId("NestedDateField")
            .withFieldType(fieldType("Date", "Date", null, null)).withDisplayContextParameter("#DATETIMEENTRY(MM)").build();
        CaseField caseField3 = newCaseField().withId("NestedCollectionTextField")
            .withFieldType(fieldType("Collection", "Collection", null, fieldType())).build();
        CaseField caseField4 = newCaseField().withId("LabelField")
            .withFieldType(fieldType("Label", "Label", null, null)).build();
        CaseField caseField5 = newCaseField().withId("ComplexNestedField")
            .withFieldType(fieldType("Complex", "Complex", Arrays.asList(caseField2, caseField3, caseField4), null)).build();
        CaseField caseField6 = newCaseField().withId("ComplexField")
            .withFieldType(fieldType("Complex", "Complex", Arrays.asList(caseField1, caseField5), null)).build();
        CaseViewField caseViewField = caseViewField(ID, null,
            fieldType("Complex", "Complex", Arrays.asList(caseField1, caseField5), null)
        );

        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("yyyy", "2001")).thenReturn("2001-01-01T00:00:00.000");
        Mockito.when(dateTimeFormatParser.convertDateToIso8601("MM", "12")).thenReturn("1970-12-01");

        JsonNode result = dateTimeEntryProcessor.execute(node, caseField6, new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isObject(), is(true)),
            () -> assertThat(result.size(), is(2)),
            () -> assertThat(result.get("ComplexDateTimeField").asText(), is("2001-01-01T00:00:00.000")),
            () -> assertThat(result.get("ComplexNestedField").get("NestedDateField").asText(), is("1970-12-01")),
            () -> assertThat(result.get("ComplexNestedField").get("NestedCollectionTextField").isArray(), is(true)),
            () -> assertThat(result.get("ComplexNestedField").get("NestedCollectionTextField").size(), is(0))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateTimeFormat() throws IOException {
        String json = "{\"DateTimeField\":\"abc\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "abc")).thenThrow(DateTimeParseException.class);

        DataProcessingException exception = assertThrows(DataProcessingException.class, () -> {
            dateTimeEntryProcessor.execute(node, caseField("FieldId"), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));
        });

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or yyyy-MM-dd'T'HH:mm:ss.SSS"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateFormat() throws IOException {
        String json = "{\"DateField\":\"abc\"}";
        JsonNode node = MAPPER.readTree(json).get("DateField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType("Date", "Date", Collections.emptyList(), null));
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateToIso8601("dd/MM/yyyy", "abc")).thenThrow(DateTimeParseException.class);

        DataProcessingException exception = assertThrows(DataProcessingException.class, () -> {
            dateTimeEntryProcessor.execute(node, caseField("FieldId"), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));
        });

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or yyyy-MM-dd"))
        );
    }

    private CaseField caseField(String id) {
        CaseField caseField = new CaseField();
        caseField.setId(id);
        return caseField;
    }

    private WizardPageField wizardPageField(String id, List<WizardPageComplexFieldOverride> overrides) {
        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId(id);
        wizardPageField.setComplexFieldOverrides(overrides);
        return wizardPageField;
    }

    private CaseViewField caseViewField(String id, String displayContextParameter, FieldType fieldType) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(id);
        caseViewField.setDisplayContextParameter(displayContextParameter);
        caseViewField.setFieldType(fieldType);
        return caseViewField;
    }

    private FieldType fieldType(String id, String type, List<CaseField> complexFields, FieldType collectionFieldType) {
        FieldType fieldType = new FieldType();
        fieldType.setId(id);
        fieldType.setType(type);
        fieldType.setComplexFields(complexFields);
        fieldType.setCollectionFieldType(collectionFieldType);
        return fieldType;
    }

    private FieldType fieldType() {
        return fieldType("DateTime");
    }

    private FieldType fieldType(String fieldType) {
        return fieldType(fieldType, fieldType, Collections.emptyList(), null);
    }

    private void setUpBaseTypes() {
        BaseType.register(new BaseType(fieldType(DATE_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(DATETIME_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COLLECTION_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COMPLEX_FIELD_TYPE)));
    }
}
