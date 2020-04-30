package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DateTimeEntryProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ID = "FieldId";

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
    }

    @Test
    void shouldReturnProcessedNodeForSimpleField() throws IOException {
        setUpBaseType("DateTime");
        String json = "{\"DateTimeField\":\"13/03/2020\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "13/03/2020")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseFieldDefinition(), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForSimpleFieldNullDisplayContextParameter() throws IOException {
        setUpBaseType("DateTime");
        String json = "{\"DateTimeField\":\"2020-03-13T00:00:00.000\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, null, fieldType());
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601(null, "2020-03-13T00:00:00.000")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseFieldDefinition(), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForSimpleFieldIncorrectDisplayContextParameter() throws IOException {
        setUpBaseType("DateTime");
        String json = "{\"DateTimeField\":\"2020-03-13T00:00:00.000\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "2020-03-13T00:00:00.000")).thenReturn("2020-03-13T00:00:00.000");

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseFieldDefinition(), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));

        assertAll(
            () -> assertThat(result.isTextual(), is(true)),
            () -> assertThat(result.asText(), is("2020-03-13T00:00:00.000"))
        );
    }

    @Test
    void shouldReturnProcessedNodeForCollectionField() throws IOException {
        setUpBaseType("Collection");
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

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseFieldDefinition(), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));

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
    void shouldThrowErrorDetailingExpectedDateTimeFormat() throws IOException {
        setUpBaseType("DateTime");
        String json = "{\"DateTimeField\":\"abc\"}";
        JsonNode node = MAPPER.readTree(json).get("DateTimeField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType());
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "abc")).thenThrow(DateTimeParseException.class);

        DataProcessingException exception = assertThrows(DataProcessingException.class, () -> {
            dateTimeEntryProcessor.execute(node, caseField("FieldId"), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));
        });

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or yyyy-MM-dd'T'HH:mm:ss.SSS"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateFormat() throws IOException {
        setUpBaseType("Date");
        String json = "{\"DateField\":\"abc\"}";
        JsonNode node = MAPPER.readTree(json).get("DateField");
        CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType("Date", "Date", Collections.emptyList(), null));
        Mockito.when(caseViewFieldBuilder.build(Mockito.any(), Mockito.any())).thenReturn(caseViewField);
        Mockito.when(dateTimeFormatParser.convertDateToIso8601("dd/MM/yyyy", "abc")).thenThrow(DateTimeParseException.class);

        DataProcessingException exception = assertThrows(DataProcessingException.class, () -> {
            dateTimeEntryProcessor.execute(node, caseField("FieldId"), new CaseEventFieldDefinition(), wizardPageField(ID, Collections.EMPTY_LIST));
        });

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or yyyy-MM-dd"))
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
        return fieldType("DateTime", "DateTime", Collections.emptyList(), null);
    }

    private void setUpBaseType(String baseType) {
        when(fieldType.getType()).thenReturn(baseType);
        BaseType.register(new BaseType(fieldType));
    }
}
