package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.MONEY_GBP;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.NUMBER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.YES_OR_NO;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.ID;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;

@Component
public class DataBlockGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, JsonNode> generateData(AdditionalDataContext context) {
        Map<String, JsonNode> dataBlock = newHashMap();

        context.getTopLevelPublishables().forEach(publishableField -> {
            JsonNode originalData = publishableField.findFieldDataFromCaseData(context.getCaseDetails().getData());

            JsonNode publishData = publishableField.getDisplayContext() == DisplayContext.COMPLEX
                ? buildDataBlock(originalData, publishableField, context.getNestedPublishables())
                : buildDataBlock(originalData, publishableField.getFieldType());

            dataBlock.put(publishableField.getKey(), publishData);
        });

        return dataBlock;
    }

    /**
     * Build a filtered data block with only publishable fields.
     */
    private JsonNode buildDataBlock(JsonNode originalNode,
                                    PublishableField publishableField,
                                    List<PublishableField> nestedPublishables) {
        if (originalNode == null || originalNode.isNull()) {
            return NullNode.getInstance();
        }

        List<PublishableField> allSubFields = nestedPublishables.stream()
            .filter(field -> field.isSubFieldOf(publishableField))
            .collect(Collectors.toList());

        List<PublishableField> directChildrenFields = publishableField.filterDirectChildrenFrom(allSubFields);

        FieldTypeDefinition fieldType = publishableField.getFieldType();
        if (fieldType.isComplexFieldType()) {
            return allSubFields.isEmpty()
                ? buildComplexNode(originalNode, fieldType)
                : buildComplexNode(originalNode, allSubFields, directChildrenFields);
        } else if (fieldType.isCollectionFieldType()) {
            return buildCollectionNode(originalNode, (originalValueNode) ->
                fieldType.getCollectionFieldTypeDefinition().getChildren().isEmpty()
                    ? buildSimpleNode(originalValueNode, fieldType.getCollectionFieldTypeDefinition().getType())
                    : buildComplexNode(originalValueNode, allSubFields, directChildrenFields));
        } else {
            return buildSimpleNode(originalNode, fieldType.getType());
        }
    }

    /**
     * Build a non-filtered data block, including all subfields.
     */
    private JsonNode buildDataBlock(JsonNode originalNode, FieldTypeDefinition fieldType) {
        if (originalNode == null || originalNode.isNull()) {
            return NullNode.getInstance();
        }

        if (fieldType.isComplexFieldType()) {
            return buildComplexNode(originalNode, fieldType);
        } else if (fieldType.isCollectionFieldType()) {
            return buildCollectionNode(originalNode, fieldType);
        } else {
            return buildSimpleNode(originalNode, fieldType.getType());
        }
    }

    private JsonNode buildComplexNode(JsonNode originalNode,
                                      List<PublishableField> allSubFields,
                                      List<PublishableField> directChildrenFields) {
        ObjectNode objectNode = MAPPER.createObjectNode();
        directChildrenFields.forEach(directChild ->
            objectNode.set(directChild.getFieldId(),
                buildDataBlock(originalNode.get(directChild.getFieldId()), directChild, allSubFields)));
        return objectNode;
    }

    private JsonNode buildComplexNode(JsonNode originalNode, FieldTypeDefinition fieldType) {
        ObjectNode objectNode = MAPPER.createObjectNode();
        fieldType.getChildren().forEach(caseFieldDefinition -> {
            String fieldId = caseFieldDefinition.getId();
            if (originalNode.has(fieldId)) {
                JsonNode childNode = originalNode.get(fieldId);
                if (childNode == null || childNode.isNull()) {
                    objectNode.set(fieldId, NullNode.getInstance());
                } else {
                    objectNode.set(fieldId, buildDataBlock(childNode, caseFieldDefinition.getFieldTypeDefinition()));
                }
            }
        });
        return objectNode;
    }

    private JsonNode buildCollectionNode(JsonNode originalNode, FieldTypeDefinition fieldType) {
        List<CaseFieldDefinition> children = fieldType.getChildren();
        if (children.isEmpty()) {
            return originalNode;
        }

        return buildCollectionNode(originalNode, (originalValueNode) -> {
            ObjectNode newValueNode = MAPPER.createObjectNode();
            children.forEach(caseFieldDefinition -> {
                String fieldId = caseFieldDefinition.getId();
                if (originalValueNode.has(fieldId)) {
                    newValueNode.set(fieldId,
                        buildDataBlock(originalValueNode.get(fieldId), caseFieldDefinition.getFieldTypeDefinition()));
                }
            });
            return newValueNode;
        });
    }

    private JsonNode buildCollectionNode(JsonNode originalNode,
                                         Function<JsonNode, JsonNode> valueFunction) {
        ArrayNode arrayNode = MAPPER.createArrayNode();

        originalNode.forEach(collectionItem -> {
            ObjectNode newCollectionItem = MAPPER.createObjectNode();
            newCollectionItem.set(ID, collectionItem.get(ID));
            newCollectionItem.set(VALUE, valueFunction.apply(collectionItem.get(VALUE)));
            arrayNode.add(newCollectionItem);
        });

        return arrayNode;
    }

    private JsonNode buildSimpleNode(JsonNode node, String fieldType) {
        switch (fieldType) {
            case YES_OR_NO:
                return booleanNodeOf(node);
            case NUMBER:
            case MONEY_GBP:
                return numberNodeOf(node);
            default:
                return node;
        }
    }

    private JsonNode booleanNodeOf(JsonNode node) {
        if (isNullOrEmpty(node.textValue())) {
            return MAPPER.nullNode();
        }

        return node.textValue().equalsIgnoreCase("Yes") ? BooleanNode.TRUE : BooleanNode.FALSE;
    }

    private JsonNode numberNodeOf(JsonNode node) {
        return node.isNumber() ? LongNode.valueOf(node.asLong()) : LongNode.valueOf(Long.parseLong(node.textValue()));
    }
}
