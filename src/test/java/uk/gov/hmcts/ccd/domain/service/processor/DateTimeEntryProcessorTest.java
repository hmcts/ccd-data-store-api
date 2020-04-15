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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
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
    private FieldType fieldType;

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

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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

        JsonNode result = dateTimeEntryProcessor.execute(node, new CaseField(), new CaseEventField(), wizardPageField(ID, Collections.EMPTY_LIST));

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
        return fieldType("DateTime", "DateTime", Collections.emptyList(), null);
    }

    private void setUpBaseType(String baseType) {
        when(fieldType.getType()).thenReturn(baseType);
        BaseType.register(new BaseType(fieldType));
    }
}
