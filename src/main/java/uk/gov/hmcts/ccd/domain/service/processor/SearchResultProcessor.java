package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SearchResultProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public SearchResultProcessor(final DateTimeFormatParser dateTimeFormatParser) {
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    public SearchResultView execute(List<SearchResultViewColumn> viewColumns,
                                    List<SearchResultViewItem> viewItems,
                                    String resultError) {
        for (SearchResultViewColumn viewColumn : viewColumns) {
            if (DisplayContextParameter
                .hasDisplayContextParameterType(viewColumn.getDisplayContextParameter(), DisplayContextParameterType.DATETIMEDISPLAY)) {
                viewItems = viewItems.stream()
                    .map(viewItem -> processSearchResultViewItem(viewItem, viewColumn))
                    .collect(Collectors.toList());
            }
        }

        return new SearchResultView(viewColumns, viewItems, resultError);
    }

    private SearchResultViewItem processSearchResultViewItem(SearchResultViewItem viewItem, SearchResultViewColumn viewColumn) {
        return DisplayContextParameter.getDisplayContextParameterOfType(viewColumn.getDisplayContextParameter(), DisplayContextParameterType.DATETIMEDISPLAY)
            .map(dcp ->  {
                viewItem.getCaseFieldsFormatted().replace(viewColumn.getCaseFieldId(),
                    processNode(viewItem.getCaseFields().get(viewColumn.getCaseFieldId()), dcp.getValue(), viewColumn.getCaseFieldType()));
                return viewItem;
            })
            .orElse(viewItem);
    }

    private Object processNode(final Object object,
                               final String dateFormat,
                               final FieldType fieldType) {
        if (object instanceof TextNode && !FieldProcessor.isNullOrEmpty((TextNode) object)) {
            return createTextNodeFrom((TextNode) object, dateFormat, fieldType);
        } else if (object instanceof ArrayNode && !FieldProcessor.isNullOrEmpty((ArrayNode) object)) {
            return createArrayNodeFrom((ArrayNode) object, dateFormat, fieldType);
        }

        return object;
    }

    private JsonNode createTextNodeFrom(final TextNode originalNode, final String dateFormat, final FieldType fieldType) {
        if (Strings.isNullOrEmpty(originalNode.asText())) {
            return new TextNode(originalNode.asText());
        }
        if (fieldType.getType().equals(FieldType.DATE)) {
            return new TextNode(dateTimeFormatParser.convertIso8601ToDate(dateFormat, originalNode.asText()));
        } else {
            return new TextNode(dateTimeFormatParser.convertIso8601ToDateTime(dateFormat, originalNode.asText()));
        }
    }

    private ArrayNode createArrayNodeFrom(final ArrayNode originalNode, final String dateFormat, final FieldType fieldType) {
        ArrayNode newNode = MAPPER.createArrayNode();
        originalNode.forEach(item -> {
            JsonNode newItem = item.deepCopy();
            ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                createTextNodeFrom((TextNode) item.get(CollectionValidator.VALUE), dateFormat, fieldType));
            newNode.add(newItem);
        });

        return newNode;
    }
}
