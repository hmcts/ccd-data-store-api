package uk.gov.hmcts.ccd.domain.service.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.time.format.DateTimeParseException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class SearchInputProcessorTest {

    private static final String DATE_FIELD = "DateField";
    private static final String DATETIME_FIELD = "DateTimeField";
    private static final String TEXT_FIELD = "TextField";
    private static final String COMPLEX_FIELD = "ComplexField";
    private static final String NESTED_FIELD = "NestedField";
    private static final String COLLECTION_FIELD = "CollectionField";
    private static final String WORKBASKET_VIEW = "WORKBASKET";
    private static final String DEFAULT_VIEW = null;

    private List<CriteriaInput> criteriaInputs = new ArrayList<>();
    private MetaData metaData;

    @InjectMocks
    private SearchInputProcessor searchInputProcessor;

    @Mock
    private DateTimeFormatParser dateTimeFormatParser;

    @Mock
    private GetCriteriaOperation getCriteriaOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        CriteriaInput criteriaInput1 = new CriteriaInput();
        CriteriaInput criteriaInput2 = new CriteriaInput();
        CriteriaInput criteriaInput3 = new CriteriaInput();
        criteriaInput1.setField(field(DATE_FIELD, fieldType("Date")));
        criteriaInput2.setField(field(DATETIME_FIELD, fieldType("DateTime")));
        criteriaInput3.setField(field(TEXT_FIELD, fieldType("Text")));
        criteriaInput1.setDisplayContextParameter("#DATETIMEENTRY(yyyy)");
        criteriaInput2.setDisplayContextParameter("#DATETIMEENTRY(dd/MM/yyyy)");
        criteriaInputs.addAll(Arrays.asList(criteriaInput1, criteriaInput2, criteriaInput3));
        doReturn(criteriaInputs).when(getCriteriaOperation).execute(Mockito.any(), Mockito.any(), Mockito.any());

        metaData = new MetaData("Case Type", "Jurisdiction");
    }

    @Test
    void shouldConvertWorkbasketDateQueryParamsUsingDisplayContextParameter() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(DATE_FIELD, "2020");
        queryParams.put(DATETIME_FIELD, "30/01/1990");
        queryParams.put(TEXT_FIELD, "Text Value");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "2020")).thenReturn("2020-10-10");
        when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "30/01/1990")).thenReturn("1990-01-30");

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.WORKBASKET));
        assertAll(
            () -> assertThat(result.size(), is(3)),
            () -> assertThat(result.get(DATE_FIELD), is("2020-10-10")),
            () -> assertThat(result.get(DATETIME_FIELD), is("1990-01-30")),
            () -> assertThat(result.get(TEXT_FIELD), is("Text Value"))
        );
    }

    @Test
    void shouldConvertSearchDateQueryParamsUsingDisplayContextParameter() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(DATE_FIELD, "2020");
        queryParams.put(DATETIME_FIELD, "30/01/1990");
        queryParams.put(TEXT_FIELD, "Text Value");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "2020")).thenReturn("2020-10-10");
        when(dateTimeFormatParser.convertDateTimeToIso8601("dd/MM/yyyy", "30/01/1990")).thenReturn("1990-01-30");

        Map<String, String> result = searchInputProcessor.execute(DEFAULT_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.SEARCH));
        assertAll(
            () -> assertThat(result.size(), is(3)),
            () -> assertThat(result.get(DATE_FIELD), is("2020-10-10")),
            () -> assertThat(result.get(DATETIME_FIELD), is("1990-01-30")),
            () -> assertThat(result.get(TEXT_FIELD), is("Text Value"))
        );
    }

    @Test
    void shouldConvertWorkbasketComplexQueryParamUsingDisplayContextParameter() {
        CriteriaInput criteriaInput = new CriteriaInput();
        FieldType complexFieldType = aFieldType().withId(COMPLEX_FIELD).withType(FieldType.COMPLEX).withComplexField(
            newCaseField().withId(NESTED_FIELD).withFieldType(fieldType("Date")).withDisplayContextParameter("#DATETIMEENTRY(yyyy)").build()
        ).build();
        criteriaInput.setField(field(COMPLEX_FIELD, complexFieldType));
        criteriaInputs.add(criteriaInput);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ComplexField.NestedField", "2020");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "2020")).thenReturn("2020-01-01");

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.WORKBASKET));
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("ComplexField.NestedField"), is("2020-01-01"))
        );
    }

    @Test
    void shouldConvertSearchComplexQueryParamUsingDisplayContextParameter() {
        CriteriaInput criteriaInput = new CriteriaInput();
        FieldType complexFieldType = aFieldType().withId(COMPLEX_FIELD).withType(FieldType.COMPLEX).withComplexField(
            newCaseField().withId(NESTED_FIELD).withFieldType(fieldType("Date")).withDisplayContextParameter("#DATETIMEENTRY(yyyy)").build()
        ).build();
        criteriaInput.setField(field(COMPLEX_FIELD, complexFieldType));
        criteriaInputs.add(criteriaInput);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ComplexField.NestedField", "2020");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "2020")).thenReturn("2020-01-01");

        Map<String, String> result = searchInputProcessor.execute(DEFAULT_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.SEARCH));
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("ComplexField.NestedField"), is("2020-01-01"))
        );
    }

    @Test
    void shouldConvertWorkbasketListElementCodeQueryParamUsingDisplayContextParameter() {
        CriteriaInput criteriaInput = new CriteriaInput();
        criteriaInput.setField(field(COMPLEX_FIELD, NESTED_FIELD, fieldType("DateTime")));
        criteriaInput.setDisplayContextParameter("#DATETIMEENTRY(yyyy)");
        criteriaInputs.add(criteriaInput);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ComplexField.NestedField", "2020");
        when(dateTimeFormatParser.convertDateTimeToIso8601("yyyy", "2020")).thenReturn("2020-01-01T00:00:00.000");

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.WORKBASKET));
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("ComplexField.NestedField"), is("2020-01-01T00:00:00.000"))
        );
    }

    @Test
    void shouldConvertSearchListElementCodeQueryParamUsingDisplayContextParameter() {
        CriteriaInput criteriaInput = new CriteriaInput();
        criteriaInput.setField(field(COMPLEX_FIELD, NESTED_FIELD, fieldType("DateTime")));
        criteriaInput.setDisplayContextParameter("#DATETIMEENTRY(yyyy)");
        criteriaInputs.add(criteriaInput);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ComplexField.NestedField", "2020");
        when(dateTimeFormatParser.convertDateTimeToIso8601("yyyy", "2020")).thenReturn("2020-01-01T00:00:00.000");

        Map<String, String> result = searchInputProcessor.execute(DEFAULT_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.SEARCH));
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("ComplexField.NestedField"), is("2020-01-01T00:00:00.000"))
        );
    }

    @Test
    void shouldConvertWorkbasketCollectionQueryParamUsingDisplayContextParameter() {
        CriteriaInput criteriaInput = new CriteriaInput();
        criteriaInput.setField(field(COLLECTION_FIELD,
            aFieldType().withId(FieldType.COLLECTION).withType(FieldType.COLLECTION).withCollectionFieldType(fieldType("Date")).build())
        );
        criteriaInput.setDisplayContextParameter("#DATETIMEENTRY(yyyy)");
        criteriaInputs.add(criteriaInput);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("CollectionField.0.value", "2020");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "2020")).thenReturn("2020-01-01");

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        verify(getCriteriaOperation).execute(eq("Case Type"), eq(null), eq(CriteriaType.WORKBASKET));
        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("CollectionField.0.value"), is("2020-01-01"))
        );
    }

    @Test
    void shouldUseExistingValueIfCriteriaInputIsNotFound() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("New Id", "New Value");

        Map<String, String> result = searchInputProcessor.execute(DEFAULT_VIEW, metaData, queryParams);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("New Id"), is("New Value"))
        );
    }

    @Test
    void shouldPassWhenNoParamsAreProvided() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(DATE_FIELD, "Invalid Value");
        when(dateTimeFormatParser.convertDateToIso8601("yyyy", "Invalid Value"))
            .thenThrow(DateTimeParseException.class);

        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process input DateField with value Invalid Value. Expected format: yyyy"))
        );
    }

    @Test
    void shouldUseQueryParamsUnchangedWhenNoDisplayContextParameters() {
        CriteriaInput criteriaInput1 = new CriteriaInput();
        criteriaInput1.setField(field(DATE_FIELD, fieldType("Date")));
        CriteriaInput criteriaInput2 = new CriteriaInput();
        criteriaInput2.setField(field(DATETIME_FIELD, fieldType("DateTime")));
        CriteriaInput criteriaInput3 = new CriteriaInput();
        criteriaInput3.setField(field(TEXT_FIELD, fieldType("Text")));
        CriteriaInput criteriaInput4 = new CriteriaInput();
        criteriaInput4.setField(field(COLLECTION_FIELD,
            aFieldType().withId(FieldType.COLLECTION).withType(FieldType.COLLECTION).withCollectionFieldType(fieldType("Date")).build())
        );
        CriteriaInput criteriaInput5 = new CriteriaInput();
        criteriaInput5.setField(field(COMPLEX_FIELD, NESTED_FIELD, fieldType("DateTime")));
        CriteriaInput criteriaInput6 = new CriteriaInput();
        criteriaInput6.setField(field(COMPLEX_FIELD, aFieldType().withId(COMPLEX_FIELD).withType(FieldType.COMPLEX).withComplexField(
            newCaseField().withId("OtherNestedField").withFieldType(fieldType("Date")).build()
        ).build()));
        criteriaInputs = Arrays.asList(criteriaInput1, criteriaInput2, criteriaInput3, criteriaInput4, criteriaInput5, criteriaInput6);
        doReturn(criteriaInputs).when(getCriteriaOperation).execute(Mockito.any(), Mockito.any(), Mockito.any());

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(DATE_FIELD, "2001-01-01");
        queryParams.put(DATETIME_FIELD, "2002-02-02T02:02:02.000");
        queryParams.put(TEXT_FIELD, "Text Value");
        queryParams.put("ComplexField.NestedField", "2004-04-04");
        queryParams.put("ComplexField.OtherNestedField", "2005-05-05");
        queryParams.put("CollectionField.0.value", "2006-06-06");

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        verifyNoMoreInteractions(dateTimeFormatParser);
        assertAll(
            () -> assertThat(result.size(), is(6)),
            () -> assertThat(result.get(DATE_FIELD), is("2001-01-01")),
            () -> assertThat(result.get(DATETIME_FIELD), is("2002-02-02T02:02:02.000")),
            () -> assertThat(result.get(TEXT_FIELD), is("Text Value")),
            () -> assertThat(result.get("ComplexField.NestedField"), is("2004-04-04")),
            () -> assertThat(result.get("ComplexField.OtherNestedField"), is("2005-05-05")),
            () -> assertThat(result.get("CollectionField.0.value"), is("2006-06-06"))
        );
    }

    @Test
    void shouldThrowDataProcessingException() {
        Map<String, String> queryParams = new HashMap<>();

        Map<String, String> result = searchInputProcessor.execute(WORKBASKET_VIEW, metaData, queryParams);

        assertAll(
            () -> assertThat(result.size(), is(0))
        );
    }

    private Field field(String id, FieldType fieldType) {
        Field field = new Field();
        field.setId(id);
        field.setType(fieldType);
        return field;
    }

    private Field field(String id, String elementPath, FieldType fieldType) {
        Field field = field(id, fieldType);
        field.setElementPath(elementPath);
        return field;
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
}
