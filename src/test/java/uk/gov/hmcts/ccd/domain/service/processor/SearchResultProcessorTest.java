package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

class SearchResultProcessorTest {

    private static final String DATE_FIELD = "DateField";
    private static final String DATETIME_FIELD = "DateTimeField";
    private static final String TEXT_FIELD = "TextField";

    private List<SearchResultViewColumn> viewColumns = new ArrayList<>();
    private List<SearchResultViewItem> viewItems = new ArrayList<>();
    private Map<String, Object> caseFields = new HashMap<>();

    @InjectMocks
    private SearchResultProcessor searchResultProcessor;

    @Mock
    private DateTimeFormatParser dateTimeFormatParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        SearchResultViewColumn column1 = new SearchResultViewColumn(DATE_FIELD,
            fieldType("Date"), null, 1, false, "#DATETIMEDISPLAY(dd/MM/yyyy)");
        SearchResultViewColumn column2 = new SearchResultViewColumn(DATETIME_FIELD,
            fieldType("DateTime"), null, 1, false, "#DATETIMEDISPLAY(ddMMyyyy)");
        viewColumns.addAll(Arrays.asList(column1, column2));

        caseFields.put(DATE_FIELD, new TextNode("2020-10-01"));
        caseFields.put(DATETIME_FIELD, new TextNode("1985-12-30"));
        caseFields.put(TEXT_FIELD, new TextNode("Text Value"));
        SearchResultViewItem item = new SearchResultViewItem("CaseId", caseFields);
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
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATE_FIELD)).asText(), is("01/10/2020")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(DATETIME_FIELD)).asText(), is("30121985")),
            () -> assertThat(((TextNode)itemResult.getCaseFields().get(TEXT_FIELD)).asText(), is("Text Value"))
        );
    }

    @Test
    void shouldUseOriginalValueForDatesWithoutDCP() {
        SearchResultViewColumn column1 = new SearchResultViewColumn(DATE_FIELD,
            fieldType("Date"), null, 1, false, null);
        SearchResultViewColumn column2 = new SearchResultViewColumn(DATETIME_FIELD,
            fieldType("DateTime"), null, 1, false, null);
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
