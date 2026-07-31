package uk.gov.hmcts.ccd.domain.service.processor.date;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CommonViewHeader;
import uk.gov.hmcts.ccd.domain.model.search.CommonViewItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.config.JacksonUtils.asText;
import static uk.gov.hmcts.ccd.config.JacksonUtils.stringOrNullNode;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.DATETIMEDISPLAY;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.service.processor.FieldProcessor.isNullOrEmpty;
import static uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser.DATE_TIME_FORMAT;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;

@Component
public class DateTimeSearchResultProcessor {

    protected static final ObjectMapper MAPPER = JsonMapper.builderWithJackson2Defaults().build();
    private static final String FIELD_SEPARATOR = ".";

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public DateTimeSearchResultProcessor(final DateTimeFormatParser dateTimeFormatParser) {
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    public <T extends CommonViewHeader, U extends CommonViewItem> List<U> execute(List<T> viewHeaders,
                                                                                  List<U> viewItems) {
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
        if (object instanceof StringNode && !isNullOrEmpty((StringNode) object)) {
            return createTextNodeFrom((StringNode) object, viewHeader, viewHeader.getCaseFieldId());
        } else if (object instanceof ArrayNode && !isNullOrEmpty((ArrayNode) object)) {
            return createArrayNodeFrom((ArrayNode) object, viewHeader, viewHeader.getCaseFieldId());
        } else if (object instanceof ObjectNode && !isNullOrEmpty((ObjectNode) object)) {
            return createObjectNodeFrom((ObjectNode) object,
                viewHeader,
                viewHeader.getCaseFieldTypeDefinition().getComplexFields(),
                viewHeader.getCaseFieldId());
        } else if (object instanceof LocalDateTime) {
            return createTextNodeFrom(new StringNode(((LocalDateTime) object)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))), viewHeader, viewHeader.getCaseFieldId());
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
        complexCaseFields.forEach(complexCaseField -> {
            final String test = complexCaseField.getFieldTypeDefinition().getType();
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = originalNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (isNullOrEmpty(caseFieldNode)) {
                newNode.set(fieldId, caseFieldNode);
            } else if (complexCaseField.isCollectionFieldType()) {
                newNode.set(fieldId,
                    createArrayNodeFrom((ArrayNode) caseFieldNode, viewHeader, fieldPath));
            } else if (complexCaseField.isComplexFieldType()) {
                Optional.ofNullable(
                    createObjectNodeFrom((ObjectNode) caseFieldNode, viewHeader,
                        complexCaseField.getFieldTypeDefinition().getComplexFields(), fieldPath))
                    .ifPresent(result -> newNode.set(fieldId, result));
            } else {
                newNode.set(fieldId, createTextNodeFrom((StringNode) caseFieldNode, viewHeader, fieldPath));
            }
        });

        return newNode;
    }

    private JsonNode createTextNodeFrom(final StringNode originalNode,
                                        final CommonViewHeader viewHeader,
                                        final String fieldPath) {
        if (Strings.isNullOrEmpty(asText(originalNode))) {
            return new StringNode(asText(originalNode));
        }

        Optional<CommonField> nestedField =
            viewHeader.getCaseFieldTypeDefinition().getNestedField(fieldPath, true);
        CommonDCPModel dcpObject = nestedField.map(CommonDCPModel.class::cast).orElse(viewHeader);

        return dcpObject.getDisplayContextParameter(DATETIMEDISPLAY)
            .map(dcp -> {
                String fieldType = nestedField
                    .map(CommonField::getFieldTypeDefinition)
                    .map(FieldTypeDefinition::getType)
                    .orElseGet(() -> {
                        FieldTypeDefinition collectionFieldType =
                            viewHeader.getCaseFieldTypeDefinition().getCollectionFieldTypeDefinition();
                        return collectionFieldType == null ? viewHeader.getCaseFieldTypeDefinition().getType()
                            : collectionFieldType.getType();
                    });
                if (fieldType.equals(DATETIME) || viewHeader.isMetadata()) {
                    return stringOrNullNode(dateTimeFormatParser.convertIso8601ToDateTime(dcp.getValue(),
                        asText(originalNode)));
                } else {
                    return stringOrNullNode(dateTimeFormatParser.convertIso8601ToDate(dcp.getValue(),
                        asText(originalNode)));
                }
            }).orElse(new StringNode(asText(originalNode)));
    }

    private ArrayNode createArrayNodeFrom(final ArrayNode originalNode,
                                          final CommonViewHeader viewHeader,
                                          final String fieldPrefix) {
        ArrayNode newNode = MAPPER.createArrayNode();
        originalNode.forEach(item -> {
            JsonNode newItem = item.deepCopy();
            if (newItem.isObject()) {
                ((ObjectNode)newItem).replace(VALUE,
                    createCollectionValue(item.get(VALUE), viewHeader, fieldPrefix));
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
        return existingValue instanceof StringNode
            ? createTextNodeFrom((StringNode) existingValue, viewHeader, fieldPrefix) :
            createObjectNodeFrom((ObjectNode) existingValue,
                                viewHeader,
                                viewHeader.getCaseFieldTypeDefinition().getChildren(),
                                fieldPrefix);
    }
}
