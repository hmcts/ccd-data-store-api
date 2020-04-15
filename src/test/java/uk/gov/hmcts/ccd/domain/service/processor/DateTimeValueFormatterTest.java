package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.io.IOException;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DateTimeValueFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ID = "FieldId";
    private static final String DATETIME_FIELD_TYPE = "DateTime";
    private static final String DATE_FIELD_TYPE = "Date";
    private static final String COLLECTION_FIELD_TYPE = "Collection";
    private static final String COMPLEX_FIELD_TYPE = "Complex";
    private static final String TEST_DATETIME = "2020-03-13T00:00:00.000";
    private static final String TEST_DATE = "2020-03-13";
    private static final String TEST_FORMAT = "dd/MM/yyyy";

    @InjectMocks
    private DateTimeValueFormatter dateTimeValueFormatter;

    @Mock
    private DateTimeFormatParser dateTimeFormatParser;

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

    @Nested
    @DisplayName("execute(CaseViewField) tests")
    class ExecuteCaseViewFieldTest {

        @Test
        void shouldFormatSimpleDateTimeFieldUsingDisplayDCP() {
            setUpBaseType(DATETIME_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATETIME);
            CaseViewField caseViewField =
                caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(DATETIME_FIELD_TYPE), value, DisplayContext.READONLY.name());
            when(dateTimeFormatParser.convertIso8601ToDateTime(TEST_FORMAT, TEST_DATETIME)).thenReturn("13/03/2020");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is("13/03/2020"))
            );
        }

        @Test
        void shouldFormatSimpleDateFieldUsingDisplayDCP() {
            setUpBaseType(DATE_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATE);
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEDISPLAY(d M yy)", fieldType(DATE_FIELD_TYPE), value, DisplayContext.READONLY.name());
            when(dateTimeFormatParser.convertIso8601ToDate("d M yy", TEST_DATE)).thenReturn("13 3 20");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is("13 3 20"))
            );
        }

        @Test
        void shouldFormatCollectionOfDateFieldsUsingDisplayDCP() throws IOException {
            setUpBaseType(COLLECTION_FIELD_TYPE);
            ArrayNode value = collectionValue();
            CaseViewField caseViewField =
                caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), value, null);
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, TEST_DATE)).thenReturn("13/03/2020");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2010-10-30")).thenReturn("30/10/2010");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ArrayNode.class)),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).size(), is(2)),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("id").asText(), is("id1")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("value").asText(), is("13/03/2020")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("id").asText(), is("id2")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("value").asText(), is("30/10/2010"))
            );
        }

        @Test
        void shouldFormatComplexFieldsUsingComplexTypeDisplayDCP() throws IOException {
            setUpBaseType(COMPLEX_FIELD_TYPE);
            ObjectNode value = complexValue();
            CaseViewField caseViewField = caseViewField(ID, null, fieldType(COMPLEX_FIELD_TYPE), value, null);
            CaseField complexDateField = caseField("ComplexDateField", fieldType(DATE_FIELD_TYPE), "#DATETIMEENTRY(d)");
            CaseField complexNestedField = caseField("ComplexNestedField", fieldType(COMPLEX_FIELD_TYPE), null);
            CaseField nestedDateField = caseField("NestedDateField", fieldType(DATE_FIELD_TYPE), "#DATETIMEENTRY(yyyy),#DATETIMEDISPLAY(MM yyyy)");
            CaseField nestedCollectionDateField =
                caseField("NestedCollectionDateField", fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), "#DATETIMEDISPLAY(dd/MM/yyyy),#DATETIMEENTRY(yyyy)");
            complexNestedField.getFieldType().setComplexFields(Arrays.asList(nestedDateField, nestedCollectionDateField));
            caseViewField.getFieldType().setComplexFields(Arrays.asList(complexDateField, complexNestedField));
            when(dateTimeFormatParser.convertIso8601ToDate("MM yyyy", TEST_DATE)).thenReturn("03 2020");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2010-01-30")).thenReturn("30/01/2010");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2020-10-10")).thenReturn("10/10/2020");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            String expectedResultJson = "{"
                    + "\"ComplexDateField\":\"1990-01-01\","
                    + "\"ComplexNestedField\":{"
                    + "\"NestedDateField\":\"03 2020\","
                    + "\"NestedCollectionDateField\":["
                    + "{\"id\":\"id1\",\"value\":\"10/10/2020\"},"
                    + "{\"id\":\"id2\",\"value\":\"30/01/2010\"}"
                + "]}}";
            ObjectNode expectedValue = (ObjectNode) MAPPER.readTree(expectedResultJson);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ObjectNode.class)),
                () -> assertThat(result.getFormattedValue(), is(expectedValue))
            );
        }

        @Test
        void shouldReturnExistingSimpleValueWhenEntryDCP() {
            setUpBaseType(COLLECTION_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATE);
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(hhmmss)", fieldType(DATE_FIELD_TYPE), value, DisplayContext.READONLY.name());

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(result.getFormattedValue(), is(result.getValue())),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is(TEST_DATE))
            );
        }

        @Test
        void shouldReturnExistingSimpleValueWhenNoDCP() {
            setUpBaseType(COLLECTION_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATE);
            CaseViewField caseViewField = caseViewField(ID, null, fieldType(DATE_FIELD_TYPE), value, DisplayContext.READONLY.name());

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(result.getFormattedValue(), is(result.getValue())),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is(TEST_DATE))
            );
        }

        @Test
        void shouldReturnExistingCollectionValueWhenNoDCP() throws IOException {
            setUpBaseType(COLLECTION_FIELD_TYPE);
            ArrayNode value = collectionValue();
            CaseViewField caseViewField = caseViewField(ID, null, fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), value, DisplayContext.READONLY.name());

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ArrayNode.class)),
                () -> assertThat(result.getFormattedValue(), is(result.getValue())),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).size(), is(2)),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("id").asText(), is("id1")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("value").asText(), is("2020-03-13")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("id").asText(), is("id2")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("value").asText(), is("2010-10-30"))
            );
        }

        @Test
        void shouldReturnExistingComplexValueWhenNoDCP() throws IOException {
            setUpBaseType(COMPLEX_FIELD_TYPE);
            ObjectNode value = complexValue();
            CaseViewField caseViewField = caseViewField(ID, null, fieldType(COMPLEX_FIELD_TYPE), value, DisplayContext.READONLY.name());
            CaseField complexDateField = caseField("ComplexDateField", fieldType(DATE_FIELD_TYPE), null);
            CaseField complexNestedField = caseField("ComplexNestedField", fieldType(COMPLEX_FIELD_TYPE), null);
            CaseField nestedDateField = caseField("NestedDateField", fieldType(DATE_FIELD_TYPE), null);
            CaseField nestedCollectionDateField = caseField("NestedCollectionDateField", fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), null);
            complexNestedField.getFieldType().setComplexFields(Arrays.asList(nestedDateField, nestedCollectionDateField));
            caseViewField.getFieldType().setComplexFields(Arrays.asList(complexDateField, complexNestedField));

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ObjectNode.class)),
                () -> assertThat(result.getFormattedValue(), is(result.getValue()))
            );
        }

        @Test
        void shouldReturnExistingValueForUnsupportedSimpleFieldType() {
            setUpBaseType("Text");
            TextNode value = new TextNode("TextField");
            CaseViewField caseViewField = caseViewField(ID, null, fieldType("Text"), value, DisplayContext.READONLY.name());

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(result.getFormattedValue(), is(result.getValue())),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is("TextField"))
            );
        }

        @Test
        void shouldThrowDataProcessingExceptionWhenDateTimeCannotBeConverted() {
            setUpBaseType(DATETIME_FIELD_TYPE);
            TextNode value = new TextNode("INVALID");
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(DATETIME_FIELD_TYPE), value, DisplayContext.READONLY.name());
            when(dateTimeFormatParser.convertIso8601ToDateTime("dd/MM/yyyy", "INVALID")).thenThrow(DateTimeException.class);

            DataProcessingException exception = assertThrows(DataProcessingException.class,
                () -> dateTimeValueFormatter.execute(caseViewField)
            );

            assertAll(
                () -> assertThat(exception.getDetails(),
                    is("Unable to process field FieldId with value INVALID. Expected format to be either dd/MM/yyyy or yyyy-MM-dd'T'HH:mm:ss.SSS"))
            );
        }

        @Test
        void shouldThrowDataProcessingExceptionWhenDateCannotBeConverted() {
            setUpBaseType(DATE_FIELD_TYPE);
            TextNode value = new TextNode("INVALID");
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(DATE_FIELD_TYPE), value, DisplayContext.READONLY.name());
            when(dateTimeFormatParser.convertIso8601ToDate("dd/MM/yyyy", "INVALID")).thenThrow(DateTimeException.class);

            DataProcessingException exception = assertThrows(DataProcessingException.class,
                () -> dateTimeValueFormatter.execute(caseViewField)
            );

            assertAll(
                () -> assertThat(exception.getDetails(),
                    is("Unable to process field FieldId with value INVALID. Expected format to be either dd/MM/yyyy or yyyy-MM-dd"))
            );
        }
    }

    @Nested
    @DisplayName("execute(CaseViewField, WizardPageField) tests")
    class ExecuteCaseViewFieldAndWizardPageFieldTest {

        @Test
        void shouldFormatSimpleDateFieldUsingDisplayDCPWhenDisplayContextIsReadonly() {
            setUpBaseType(DATE_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATE);
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(DATE_FIELD_TYPE), value, DisplayContext.READONLY.name());
            WizardPageField wizardPageField = wizardPageField(ID, Collections.emptyList());
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, TEST_DATE)).thenReturn("13/03/2020");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField, wizardPageField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is("13/03/2020"))
            );
        }

        @Test
        void shouldFormatSimpleDateFieldUsingEntryDCPWhenDisplayContextIsMandatory() {
            setUpBaseType(DATE_FIELD_TYPE);
            TextNode value = new TextNode(TEST_DATE);
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEENTRY(dd/MM/yyyy)", fieldType(DATE_FIELD_TYPE), value, DisplayContext.MANDATORY.name());
            WizardPageField wizardPageField = wizardPageField(ID, Collections.emptyList());
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, TEST_DATE)).thenReturn("13/03/2020");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField, wizardPageField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(TextNode.class)),
                () -> assertThat(((TextNode) result.getFormattedValue()).asText(), is("13/03/2020"))
            );
        }

        @Test
        void shouldFormatCollectionOfDateFieldsUsingDisplayDCP() throws IOException {
            setUpBaseType(COLLECTION_FIELD_TYPE);
            ArrayNode value = collectionValue();
            CaseViewField caseViewField = caseViewField(ID, "#DATETIMEDISPLAY(dd/MM/yyyy)", fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), value, DisplayContext.READONLY.name());
            WizardPageField wizardPageField = wizardPageField(ID, Collections.emptyList());
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, TEST_DATE)).thenReturn("13/03/2020");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2010-10-30")).thenReturn("30/10/2010");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ArrayNode.class)),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).size(), is(2)),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("id").asText(), is("id1")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(0).get("value").asText(), is("13/03/2020")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("id").asText(), is("id2")),
                () -> assertThat(((ArrayNode) result.getFormattedValue()).get(1).get("value").asText(), is("30/10/2010"))
            );
        }

        @Test
        void shouldFormatComplexFieldsWithNoDisplayContextOverrides() throws IOException {
            setUpBaseType(COMPLEX_FIELD_TYPE);
            ObjectNode value = complexValue();
            CaseViewField caseViewField =
                caseViewField(ID, null, fieldType(COMPLEX_FIELD_TYPE), value, DisplayContext.READONLY.name());
            CaseField complexDateField = caseField("ComplexDateField",
                fieldType(DATE_FIELD_TYPE), null);
            CaseField complexNestedField = caseField("ComplexNestedField",
                fieldType(COMPLEX_FIELD_TYPE), null);
            CaseField nestedDateField = caseField("NestedDateField",
                fieldType(DATE_FIELD_TYPE), "#DATETIMEENTRY(yyyy),#DATETIMEDISPLAY(MM yyyy)");
            CaseField nestedCollectionDateField = caseField("NestedCollectionDateField",
                fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), "#DATETIMEDISPLAY(dd/MM/yyyy),#DATETIMEENTRY(yyyy)");
            complexNestedField.getFieldType().setComplexFields(Arrays.asList(nestedDateField, nestedCollectionDateField));
            caseViewField.getFieldType().setComplexFields(Arrays.asList(complexDateField, complexNestedField));
            WizardPageField wizardPageField = wizardPageField(ID, Collections.emptyList());
            when(dateTimeFormatParser.convertIso8601ToDate("MM yyyy", TEST_DATE)).thenReturn("03 2020");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2010-01-30")).thenReturn("30/01/2010");
            when(dateTimeFormatParser.convertIso8601ToDate(TEST_FORMAT, "2020-10-10")).thenReturn("10/10/2020");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField, wizardPageField);

            String expectedResultJson = "{\"ComplexDateField\":\"1990-01-01\","
                                        + "\"ComplexNestedField\":{"
                                            + "\"NestedDateField\":\"03 2020\","
                                            + "\"NestedCollectionDateField\":["
                                                + "{\"id\":\"id1\",\"value\":\"10/10/2020\"},"
                                                + "{\"id\":\"id2\",\"value\":\"30/01/2010\"}"
                                        + "]}}";
            ObjectNode expectedValue = (ObjectNode) MAPPER.readTree(expectedResultJson);

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ObjectNode.class)),
                () -> assertThat(result.getFormattedValue(), is(expectedValue))
            );
        }

        @Test
        void shouldFormatComplexFieldsWithDisplayContextOverrides() throws IOException {
            setUpBaseType(COMPLEX_FIELD_TYPE);
            ObjectNode value = complexValue();
            CaseViewField caseViewField = caseViewField(ID, null, fieldType(COMPLEX_FIELD_TYPE), value, DisplayContext.READONLY.name());
            CaseField complexDateField = caseField("ComplexDateField", fieldType(DATE_FIELD_TYPE), "#DATETIMEENTRY(d)");
            CaseField complexNestedField = caseField("ComplexNestedField", fieldType(COMPLEX_FIELD_TYPE), null);
            CaseField nestedDateField = caseField("NestedDateField", fieldType(DATE_FIELD_TYPE), "#DATETIMEENTRY(yyyy),#DATETIMEDISPLAY(MM yyyy)");
            CaseField nestedCollectionDateField =
                caseField("NestedCollectionDateField", fieldType(COLLECTION_FIELD_TYPE, fieldType(DATE_FIELD_TYPE)), "#DATETIMEDISPLAY(dd/MM/yyyy),#DATETIMEENTRY(MM)");
            complexNestedField.getFieldType().setComplexFields(Arrays.asList(nestedDateField, nestedCollectionDateField));
            caseViewField.getFieldType().setComplexFields(Arrays.asList(complexDateField, complexNestedField));
            WizardPageComplexFieldOverride nestedDateFieldOverride =
                override("FieldId.ComplexNestedField.NestedDateField", DisplayContext.MANDATORY.name());
            WizardPageComplexFieldOverride nestedCollectionDateFieldOverride =
                override("FieldId.ComplexNestedField.NestedCollectionDateField", DisplayContext.MANDATORY.name());
            WizardPageComplexFieldOverride complexDateFieldOverride =
                override("FieldId.ComplexDateField", DisplayContext.MANDATORY.name());
            WizardPageField wizardPageField =
                wizardPageField(ID, Arrays.asList(nestedDateFieldOverride, nestedCollectionDateFieldOverride, complexDateFieldOverride));
            when(dateTimeFormatParser.convertIso8601ToDate("yyyy", TEST_DATE)).thenReturn("2020");
            when(dateTimeFormatParser.convertIso8601ToDate("MM", "2010-01-30")).thenReturn("01");
            when(dateTimeFormatParser.convertIso8601ToDate("MM", "2020-10-10")).thenReturn("10");
            when(dateTimeFormatParser.convertIso8601ToDate("d", "1990-01-01")).thenReturn("1");

            CaseViewField result = dateTimeValueFormatter.execute(caseViewField, wizardPageField);

            String expectedResultJson = "{\"ComplexDateField\":\"1\","
                                        + "\"ComplexNestedField\":{"
                                            + "\"NestedDateField\":\"2020\","
                                            + "\"NestedCollectionDateField\":["
                                                + "{\"id\":\"id1\",\"value\":\"10\"},"
                                                + "{\"id\":\"id2\",\"value\":\"01\"}"
                                        + "]}}";
            ObjectNode expectedValue = (ObjectNode) MAPPER.readTree(expectedResultJson);

            assertThat(result.getFormattedValue(), is(expectedValue));

            assertAll(
                () -> assertThat(result.getFormattedValue(), instanceOf(ObjectNode.class)),
                () -> assertThat(result.getFormattedValue(), is(expectedValue))
            );
        }
    }

    private WizardPageField wizardPageField(String id, List<WizardPageComplexFieldOverride> overrides) {
        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId(id);
        wizardPageField.setComplexFieldOverrides(overrides);
        return wizardPageField;
    }

    private WizardPageComplexFieldOverride override(String fieldPath, String displayContext) {
        WizardPageComplexFieldOverride override = new WizardPageComplexFieldOverride();
        override.setComplexFieldElementId(fieldPath);
        override.setDisplayContext(displayContext);
        return override;
    }

    private CaseField caseField(String id, FieldType fieldType, String displayContextParameter) {
        CaseField caseField = new CaseField();
        caseField.setId(id);
        caseField.setFieldType(fieldType);
        caseField.setDisplayContextParameter(displayContextParameter);
        return caseField;
    }

    private CaseViewField caseViewField(String id, String displayContextParameter, FieldType fieldType, Object value, String displayContext) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(id);
        caseViewField.setDisplayContextParameter(displayContextParameter);
        caseViewField.setFieldType(fieldType);
        caseViewField.setValue(value);
        caseViewField.setDisplayContext(displayContext);
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

    private FieldType fieldType(String fieldType) {
        return fieldType(fieldType, fieldType, Collections.emptyList(), null);
    }

    private FieldType fieldType(String fieldType, FieldType collectionFieldType) {
        return fieldType(fieldType, fieldType, Collections.emptyList(), collectionFieldType);
    }

    private void setUpBaseType(String baseType) {
        when(fieldType.getType()).thenReturn(baseType);
        BaseType.register(new BaseType(fieldType));
    }

    private ArrayNode collectionValue() throws IOException {
        String json = "[{\"id\":\"id1\",\"value\":\"2020-03-13\"},{\"id\":\"id2\",\"value\":\"2010-10-30\"}]";
        return (ArrayNode) MAPPER.readTree(json);
    }

    private ObjectNode complexValue() throws IOException {
        String json = "{"
                      + "\"ComplexDateField\":\"1990-01-01\","
                      + "\"ComplexNestedField\":{"
                            + "\"NestedDateField\":\"2020-03-13\""
                            + ",\"NestedCollectionDateField\":["
                                + "{\"id\":\"id1\",\"value\":\"2020-10-10\"},"
                                + "{\"id\":\"id2\",\"value\":\"2010-01-30\"}"
                      + "]}}";
        return (ObjectNode) MAPPER.readTree(json);
    }
}
