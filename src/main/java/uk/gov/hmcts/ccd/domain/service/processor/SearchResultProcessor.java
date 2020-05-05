package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.processor.FieldProcessor.isNullOrEmpty;

@Component
public class SearchResultProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIELD_SEPARATOR = ".";

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public SearchResultProcessor(final DateTimeFormatParser dateTimeFormatParser) {
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    public SearchResultView execute(List<SearchResultViewColumn> viewColumns,
                                    List<SearchResultViewItem> viewItems,
                                    String resultError) {
        for (SearchResultViewColumn viewColumn : viewColumns) {
            viewItems = viewItems.stream()
                .map(viewItem -> processSearchResultViewItem(viewItem, viewColumn))
                .collect(Collectors.toList());
        }

        return new SearchResultView(viewColumns, viewItems, resultError);
    }

    private SearchResultViewItem processSearchResultViewItem(SearchResultViewItem viewItem, SearchResultViewColumn viewColumn) {
        viewItem.getCaseFieldsFormatted().replace(viewColumn.getCaseFieldId(),
            processObject(viewItem.getCaseFields().get(viewColumn.getCaseFieldId()), viewColumn));
        return viewItem;
    }

    private Object processObject(final Object object,
                                 final SearchResultViewColumn viewColumn) {
        if (object instanceof TextNode && !isNullOrEmpty((TextNode) object)) {
            return createTextNodeFrom((TextNode) object, viewColumn, viewColumn.getCaseFieldId());
        } else if (object instanceof ArrayNode && !isNullOrEmpty((ArrayNode) object)) {
            return createArrayNodeFrom((ArrayNode) object, viewColumn, viewColumn.getCaseFieldId());
        } else if (object instanceof ObjectNode && !isNullOrEmpty((ObjectNode) object)) {
            return createObjectNodeFrom((ObjectNode) object, viewColumn, viewColumn.getCaseFieldType().getComplexFields(), viewColumn.getCaseFieldId());
        } else if (object instanceof LocalDateTime) {
            return createTextNodeFrom(new TextNode(((LocalDateTime) object)
                .format(DateTimeFormatter.ofPattern(DateTimeFormatParser.DATE_TIME_FORMAT))), viewColumn, viewColumn.getCaseFieldId());
        }

        return object;
    }

    private JsonNode createObjectNodeFrom(final ObjectNode originalNode,
                                          final SearchResultViewColumn viewColumn,
                                          final List<CaseField> complexCaseFields,
                                          final String fieldPrefix) {
        if (isNullOrEmpty(originalNode)) {
            return originalNode;
        }

        ObjectNode newNode = MAPPER.createObjectNode();
        complexCaseFields.stream().forEach(complexCaseField -> {
            final String test = complexCaseField.getFieldType().getType();
            final BaseType complexFieldType = BaseType.get(test);
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = originalNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (isNullOrEmpty(caseFieldNode)) {
                newNode.set(fieldId, caseFieldNode);
            } else if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(fieldId,
                    createArrayNodeFrom((ArrayNode) caseFieldNode, viewColumn, fieldPath));
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                Optional.ofNullable(
                    createObjectNodeFrom((ObjectNode) caseFieldNode, viewColumn, complexCaseField.getFieldType().getComplexFields(), fieldPath))
                    .ifPresent(result -> newNode.set(fieldId, result));
            } else {
                newNode.set(fieldId, createTextNodeFrom((TextNode) caseFieldNode, viewColumn, fieldPath));
            }
        });

        return newNode;
    }

    private JsonNode createTextNodeFrom(final TextNode originalNode,
                                        final SearchResultViewColumn viewColumn,
                                        final String fieldPath) {
        if (Strings.isNullOrEmpty(originalNode.asText())) {
            return new TextNode(originalNode.asText());
        }

        final Optional<CommonField> nestedField = viewColumn.getCaseFieldType().getNestedField(fieldPath, true);
        final String displayContextParameter = nestedField
            .map(CommonField::getDisplayContextParameter)
            .orElse(viewColumn.getDisplayContextParameter());

        return DisplayContextParameter.getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEDISPLAY)
            .map(dcp -> {
                final String fieldType = nestedField
                    .map(CommonField::getFieldType)
                    .map(FieldType::getType)
                    .orElseGet(() -> {
                        FieldType collectionFieldType = viewColumn.getCaseFieldType().getCollectionFieldType();
                        return collectionFieldType == null ? viewColumn.getCaseFieldType().getType() : collectionFieldType.getType();
                    });
                if (fieldType.equals(FieldType.DATETIME) || viewColumn.isMetadata()) {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDateTime(dcp.getValue(), originalNode.asText()));
                } else {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDate(dcp.getValue(), originalNode.asText()));
                }
            }).orElse(new TextNode(originalNode.asText()));
    }

    private ArrayNode createArrayNodeFrom(final ArrayNode originalNode,
                                          final SearchResultViewColumn viewColumn,
                                          final String fieldPrefix) {
        ArrayNode newNode = MAPPER.createArrayNode();
        originalNode.forEach(item -> {
            JsonNode newItem = item.deepCopy();
            if (newItem.isObject()) {
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    createCollectionValue(item.get(CollectionValidator.VALUE), viewColumn, fieldPrefix));
            }
            newNode.add(newItem);
        });

        return newNode;
    }

    private JsonNode createCollectionValue(JsonNode existingValue,
                                           SearchResultViewColumn viewColumn,
                                           String fieldPrefix) {
        if (isNullOrEmpty(existingValue)) {
            return existingValue;
        }
        return existingValue instanceof TextNode ?
            createTextNodeFrom((TextNode) existingValue, viewColumn, fieldPrefix) :
            createObjectNodeFrom((ObjectNode) existingValue, viewColumn, viewColumn.getCaseFieldType().getChildren(), fieldPrefix);
    }
}
