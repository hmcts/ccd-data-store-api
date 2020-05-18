package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.MULTI_SELECT_LIST;

class SearchResultProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DATE_FIELD = "DateField";
    private static final String DATETIME_FIELD = "DateTimeField";
    private static final String TEXT_FIELD = "TextField";
    private static final String COMPLEX_FIELD = "ComplexField";
    private static final String COLLECTION_FIELD = "CollectionField";

    private static final String DATETIME_FIELD_TYPE = "DateTime";
    private static final String DATE_FIELD_TYPE = "Date";
    private static final String COLLECTION_FIELD_TYPE = "Collection";
    private static final String COMPLEX_FIELD_TYPE = "Complex";

    private List<SearchResultViewColumn> viewColumns = new ArrayList<>();
    private List<SearchResultViewItem> viewItems = new ArrayList<>();
    private Map<String, Object> caseFields = new HashMap<>();

    @InjectMocks
    private SearchResultProcessor searchResultProcessor;

    @Mock
    private DateTimeFormatParser dateTimeFormatParser;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.EMPTY_LIST);
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();
        setUpBaseTypes();

        SearchResultViewColumn column1 = new SearchResultViewColumn(DATE_FIELD,
            fieldType(DATE_FIELD_TYPE), null, 1, false, "#DATETIMEDISPLAY(dd/MM/yyyy)");
        SearchResultViewColumn column2 = new SearchResultViewColumn(DATETIME_FIELD,
            fieldType(DATETIME_FIELD_TYPE), null, 1, false, "#DATETIMEDISPLAY(ddMMyyyy)");
        viewColumns.addAll(Arrays.asList(column1, column2));

        caseFields.put(DATE_FIELD, new TextNode("2020-10-01"));
        caseFields.put(DATETIME_FIELD, new TextNode("1985-12-30"));
        caseFields.put(TEXT_FIELD, new TextNode("Text Value"));
        SearchResultViewItem item = new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields));
        viewItems.add(item);
    }

    @Test
    void shouldProcessDatesWithDCP() {
        when(dateTimeFormatParser.convertIso8601ToDate("dd/MM/yyyy", "2020-10-01"))
            .thenReturn("01/10/2020");
        when(dateTimeFormatParser.convertIso8601ToDateTime("ddMMyyyy", "1985-12-30"))
            .thenReturn("30121985");

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(3)),
            () -> assertThat(((TextNode)itemResult.getCaseFieldsFormatted().get(DATE_FIELD)).asText(), is("01/10/2020")),
            () -> assertThat(((TextNode)itemResult.getCaseFieldsFormatted().get(DATETIME_FIELD)).asText(), is("30121985")),
            () -> assertThat(((TextNode)itemResult.getCaseFieldsFormatted().get(TEXT_FIELD)).asText(), is("Text Value")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATE_FIELD)).asText(), is("2020-10-01")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATETIME_FIELD)).asText(), is("1985-12-30")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(TEXT_FIELD)).asText(), is("Text Value"))
        );
    }

    @Test
    void shouldProcessMetadataDatesWithDCP() {
        final String metadataField = "[METADATA_DATE]";
        final LocalDateTime localDateTime = LocalDateTime.of(2020, 10, 1, 12, 30, 0, 0);
        caseFields.put(metadataField, localDateTime);
        viewColumns.add(new SearchResultViewColumn(metadataField,
            fieldType(DATE_FIELD_TYPE), null, 1, true, "#DATETIMEDISPLAY(dd/MM/yyyy)"));
        when(dateTimeFormatParser.convertIso8601ToDateTime("dd/MM/yyyy", "2020-10-01T12:30:00.000"))
            .thenReturn("01/10/2020");
        viewItems = Collections.singletonList(new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields)));

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(4)),
            () -> assertThat(((TextNode)itemResult.getCaseFieldsFormatted().get(metadataField)).asText(), is("01/10/2020")),
            () -> assertThat(itemResult.getCaseFields().get(metadataField), is(localDateTime))
        );
    }

    @Test
    void shouldProcessComplexTypesWithDCP() throws JsonProcessingException {
        caseFields.put(COMPLEX_FIELD, MAPPER.readTree("{\"ComplexDateField\": \"2020-10-05\"}"));
        viewItems = Collections.singletonList(new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields)));
        CaseField complexField = caseField("ComplexDateField", fieldType("Date"), "#DATETIMEDISPLAY(MM-yyyy)");
        SearchResultViewColumn complexColumn = new SearchResultViewColumn(COMPLEX_FIELD,
            fieldType(COMPLEX_FIELD_TYPE, COMPLEX_FIELD_TYPE, Collections.singletonList(complexField), null), null, 1, false, null);
        viewColumns.add(complexColumn);

        when(dateTimeFormatParser.convertIso8601ToDate("MM-yyyy", "2020-10-05")).thenReturn("10-2020");

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(4)),
            () -> assertThat(((ObjectNode)itemResult.getCaseFieldsFormatted().get(COMPLEX_FIELD)).get("ComplexDateField").asText(), is("10-2020")),
            () -> assertThat(((ObjectNode)itemResult.getCaseFields().get(COMPLEX_FIELD)).get("ComplexDateField").asText(), is("2020-10-05"))
        );
    }

    @Test
    void shouldProcessCollectionsWithDCP() throws JsonProcessingException {
        caseFields.put(COLLECTION_FIELD,
            MAPPER.readTree("[{\"id\": \"1\", \"value\": \"2020-10-05\"},{\"id\": \"2\", \"value\": \"1999-12-01\"}]"));
        viewItems = Collections.singletonList(new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields)));
        SearchResultViewColumn collectionColumn = new SearchResultViewColumn(COLLECTION_FIELD,
            fieldType(COLLECTION_FIELD_TYPE, COLLECTION_FIELD_TYPE, null, fieldType(DATE_FIELD_TYPE)), null, 1, false, "#DATETIMEDISPLAY(MM-yyyy)");
        viewColumns.add(collectionColumn);

        when(dateTimeFormatParser.convertIso8601ToDate("MM-yyyy", "2020-10-05"))
            .thenReturn("10-2020");
        when(dateTimeFormatParser.convertIso8601ToDate("MM-yyyy", "1999-12-01"))
            .thenReturn("12-1999");

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(4)),
            () -> assertThat(((ArrayNode)itemResult.getCaseFieldsFormatted().get(COLLECTION_FIELD)).get(0).get(CollectionValidator.VALUE).asText(), is("10-2020")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFieldsFormatted().get(COLLECTION_FIELD)).get(1).get(CollectionValidator.VALUE).asText(), is("12-1999")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFields().get(COLLECTION_FIELD)).get(0).get(CollectionValidator.VALUE).asText(), is("2020-10-05")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFields().get(COLLECTION_FIELD)).get(1).get(CollectionValidator.VALUE).asText(), is("1999-12-01"))
        );
    }

    @Test
    void shouldProcessCollectionOfComplexTypesWithDCP() throws JsonProcessingException {
        caseFields.put(COLLECTION_FIELD,
            MAPPER.readTree("[{\"id\": \"1\", \"value\": {\"NestedDate\": \"2020-10-05\"}},"
                            + "{\"id\": \"2\", \"value\": {\"NestedDate\": \"1992-07-30\"}}]"));
        CaseField nestedDateField = caseField("NestedDate", fieldType("Date"), "#DATETIMEDISPLAY(MM-yyyy)");
        FieldType complexFieldType = fieldType(COMPLEX_FIELD_TYPE, COMPLEX_FIELD_TYPE, Collections.singletonList(nestedDateField), null);
        SearchResultViewColumn collectionColumn = new SearchResultViewColumn(COLLECTION_FIELD,
            fieldType(COLLECTION_FIELD_TYPE, COLLECTION_FIELD_TYPE, null, complexFieldType), null, 1, false, null);
        viewItems = Collections.singletonList(new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields)));
        viewColumns.add(collectionColumn);

        when(dateTimeFormatParser.convertIso8601ToDate("MM-yyyy", "2020-10-05"))
            .thenReturn("10-2020");
        when(dateTimeFormatParser.convertIso8601ToDate("MM-yyyy", "1992-07-30"))
            .thenReturn("07-1992");

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(4)),
            () -> assertThat(((ArrayNode)itemResult.getCaseFieldsFormatted().get(COLLECTION_FIELD)).get(0).get(CollectionValidator.VALUE).get("NestedDate").asText(), is("10-2020")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFieldsFormatted().get(COLLECTION_FIELD)).get(1).get(CollectionValidator.VALUE).get("NestedDate").asText(), is("07-1992")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFields().get(COLLECTION_FIELD)).get(0).get(CollectionValidator.VALUE).get("NestedDate").asText(), is("2020-10-05")),
            () -> assertThat(((ArrayNode)itemResult.getCaseFields().get(COLLECTION_FIELD)).get(1).get(CollectionValidator.VALUE).get("NestedDate").asText(), is("1992-07-30"))
        );
    }

    @Test
    void shouldUseOriginalValueForDatesWithoutDCP() {
        SearchResultViewColumn column1 = new SearchResultViewColumn(DATE_FIELD,
            fieldType(DATE_FIELD_TYPE), null, 1, false, null);
        SearchResultViewColumn column2 = new SearchResultViewColumn(DATETIME_FIELD,
            fieldType(DATETIME_FIELD_TYPE), null, 1, false, null);
        List<SearchResultViewColumn> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(column1, column2));

        final SearchResultView result = searchResultProcessor.execute(columns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(3)),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATE_FIELD)).asText(), is("2020-10-01")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATETIME_FIELD)).asText(), is("1985-12-30"))
        );
    }

    @Test
    void shouldUseOriginalValueForMultiSelectListField() throws JsonProcessingException {
        final String multiSelectField = "MultiSelectField";
        caseFields.put(multiSelectField,
            MAPPER.readTree("[\"Value1\", \"Value2\"]"));
        SearchResultViewColumn column1 = new SearchResultViewColumn(multiSelectField,
            fieldType(MULTI_SELECT_LIST), null, 1, false, null);
        List<SearchResultViewColumn> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(column1));
        viewItems = Collections.singletonList(new SearchResultViewItem("CaseId", caseFields, new HashMap<>(caseFields)));
        viewColumns.add(column1);

        final SearchResultView result = searchResultProcessor.execute(viewColumns, viewItems, null);

        final SearchResultViewItem itemResult = result.getSearchResultViewItems().get(0);
        final ArrayNode multiSelectResult = (ArrayNode)itemResult.getCaseFields().get(multiSelectField);
        final ArrayNode multiSelectResultFormatted = (ArrayNode)itemResult.getCaseFieldsFormatted().get(multiSelectField);
        assertAll(
            () -> assertThat(result.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(itemResult.getCaseFields().size(), is(4)),
            () -> assertThat(multiSelectResult.size(), is(2)),
            () -> assertThat(multiSelectResult.get(0).asText(), is("Value1")),
            () -> assertThat(multiSelectResult.get(1).asText(), is("Value2")),
            () -> assertThat(multiSelectResultFormatted.size(), is(2)),
            () -> assertThat(multiSelectResultFormatted.get(0).asText(), is("Value1")),
            () -> assertThat(multiSelectResultFormatted.get(1).asText(), is("Value2"))
        );
    }

    private CaseField caseField(String id, FieldType fieldType, String displayContextParameter) {
        CaseField caseField = new CaseField();
        caseField.setId(id);
        caseField.setFieldType(fieldType);
        caseField.setDisplayContextParameter(displayContextParameter);
        return caseField;
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

    private void setUpBaseTypes() {
        BaseType.register(new BaseType(fieldType(DATE_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(DATETIME_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COLLECTION_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COMPLEX_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(MULTI_SELECT_LIST)));
    }
}
