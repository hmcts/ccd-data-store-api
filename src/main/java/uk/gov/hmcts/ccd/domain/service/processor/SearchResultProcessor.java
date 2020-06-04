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
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.*;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

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

    public <T extends CommonViewHeader, U extends CommonViewItem> List<U> execute(List<T> viewHeaders, List<U> viewItems) {
        for (T viewHeader : viewHeaders) {
            viewItems = viewItems.stream()
                .map(viewItem -> processViewItem(viewItem, viewHeader))
                .collect(Collectors.toList());
        }

        return viewItems;
    }

    private <T extends CommonViewHeader, U extends CommonViewItem> U processViewItem(U viewItem, T viewHeader) {
        viewItem.getFieldsFormatted().replace(viewHeader.getCaseFieldId(),
            processObject(viewItem.getFields().get(viewHeader.getCaseFieldId()), viewHeader));
        return viewItem;
    }

    private Object processObject(final Object object,
                                 final CommonViewHeader viewHeader) {
        if (object instanceof TextNode && !isNullOrEmpty((TextNode) object)) {
            return createTextNodeFrom((TextNode) object, viewHeader, viewHeader.getCaseFieldId());
        } else if (object instanceof ArrayNode && !isNullOrEmpty((ArrayNode) object)) {
            return createArrayNodeFrom((ArrayNode) object, viewHeader, viewHeader.getCaseFieldId());
        } else if (object instanceof ObjectNode && !isNullOrEmpty((ObjectNode) object)) {
            return createObjectNodeFrom((ObjectNode) object,
                viewHeader,
                viewHeader.getCaseFieldTypeDefinition().getComplexFields(),
                viewHeader.getCaseFieldId());
        } else if (object instanceof LocalDateTime) {
            return createTextNodeFrom(new TextNode(((LocalDateTime) object)
                .format(DateTimeFormatter.ofPattern(DateTimeFormatParser.DATE_TIME_FORMAT))), viewHeader, viewHeader.getCaseFieldId());
        }

        return object;
    }

    private JsonNode createObjectNodeFrom(final ObjectNode originalNode,
                                          final CommonViewHeader viewHeader,
                                          final List<CaseFieldDefinition> complexCaseFields,
                                          final String fieldPrefix) {
        if (isNullOrEmpty(originalNode)) {
            return originalNode;
        }

        ObjectNode newNode = MAPPER.createObjectNode();
        complexCaseFields.stream().forEach(complexCaseField -> {
            final String test = complexCaseField.getFieldTypeDefinition().getType();
            final BaseType complexFieldType = BaseType.get(test);
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = originalNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (isNullOrEmpty(caseFieldNode)) {
                newNode.set(fieldId, caseFieldNode);
            } else if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(fieldId,
                    createArrayNodeFrom((ArrayNode) caseFieldNode, viewHeader, fieldPath));
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                Optional.ofNullable(
                    createObjectNodeFrom((ObjectNode) caseFieldNode, viewHeader, complexCaseField.getFieldTypeDefinition().getComplexFields(), fieldPath))
                    .ifPresent(result -> newNode.set(fieldId, result));
            } else {
                newNode.set(fieldId, createTextNodeFrom((TextNode) caseFieldNode, viewHeader, fieldPath));
            }
        });

        return newNode;
    }

    private JsonNode createTextNodeFrom(final TextNode originalNode,
                                        final CommonViewHeader viewHeader,
                                        final String fieldPath) {
        if (Strings.isNullOrEmpty(originalNode.asText())) {
            return new TextNode(originalNode.asText());
        }

        final Optional<CommonField> nestedField = viewHeader.getCaseFieldTypeDefinition().getNestedField(fieldPath, true);
        final String displayContextParameter = nestedField
            .map(CommonField::getDisplayContextParameter)
            .orElse(viewHeader.getDisplayContextParameter());

        return DisplayContextParameter.getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEDISPLAY)
            .map(dcp -> {
                final String fieldType = nestedField
                    .map(CommonField::getFieldTypeDefinition)
                    .map(FieldTypeDefinition::getType)
                    .orElseGet(() -> {
                        FieldTypeDefinition collectionFieldType = viewHeader.getCaseFieldTypeDefinition().getCollectionFieldTypeDefinition();
                        return collectionFieldType == null ? viewHeader.getCaseFieldTypeDefinition().getType() : collectionFieldType.getType();
                    });
                if (fieldType.equals(FieldTypeDefinition.DATETIME) || viewHeader.isMetadata()) {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDateTime(dcp.getValue(), originalNode.asText()));
                } else {
                    return new TextNode(dateTimeFormatParser.convertIso8601ToDate(dcp.getValue(), originalNode.asText()));
                }
            }).orElse(new TextNode(originalNode.asText()));
    }

    private ArrayNode createArrayNodeFrom(final ArrayNode originalNode,
                                          final CommonViewHeader viewHeader,
                                          final String fieldPrefix) {
        ArrayNode newNode = MAPPER.createArrayNode();
        originalNode.forEach(item -> {
            JsonNode newItem = item.deepCopy();
            if (newItem.isObject()) {
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    createCollectionValue(item.get(CollectionValidator.VALUE), viewHeader, fieldPrefix));
            }
            newNode.add(newItem);
        });

        return newNode;
    }

    private JsonNode createCollectionValue(JsonNode existingValue,
                                           CommonViewHeader viewHeader,
                                           String fieldPrefix) {
        if (isNullOrEmpty(existingValue)) {
            return existingValue;
        }
        return existingValue instanceof TextNode
            ? createTextNodeFrom((TextNode) existingValue, viewHeader, fieldPrefix) :
            createObjectNodeFrom((ObjectNode) existingValue,
                                viewHeader,
                                viewHeader.getCaseFieldTypeDefinition().getChildren(),
                                fieldPrefix);
    }
}
