package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

@Service
public class CaseDataService {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";
    private static final String DEFAULT_CLASSIFICATION = "";
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";

    public Map<String, JsonNode> getDefaultSecurityClassifications(final CaseTypeDefinition caseType,
                                                                   final Map<String, JsonNode> caseData,
                                                                   final Map<String, JsonNode>
                                                                       currentDataClassification) {
        final JsonNode clonedDataClassification = cloneAndConvertDataMap(caseData);
        deduceDefaultClassifications(
            clonedDataClassification,
            JacksonUtils.convertValueJsonNode(currentDataClassification), caseType.getCaseFieldDefinitions(),
            EMPTY_STRING);
        return JacksonUtils.convertValue(clonedDataClassification);
    }

    private void deduceDefaultClassifications(final JsonNode dataNode,
                                              final JsonNode existingDataClassificationNode,
                                              final List<CaseFieldDefinition> caseFieldDefinitions,
                                              final String fieldIdPrefix) {
        final Iterator<String> fieldNames = dataNode.fieldNames();
        while (fieldNames.hasNext()) {
            boolean found = false;
            final String fieldName = fieldNames.next();
            Iterator<CaseFieldDefinition> cfIterator = caseFieldDefinitions.iterator();
            while (!found && cfIterator.hasNext()) {
                CaseFieldDefinition caseField = cfIterator.next();
                if (caseField.getId().equalsIgnoreCase(fieldName)) {
                    final String caseFieldType = caseField.getFieldTypeDefinition().getType();
                    if (caseFieldType.equalsIgnoreCase(COMPLEX)) {
                        found = true;
                        deduceClassificationForComplexType(dataNode,
                            getExistingDataClassificationNodeOrEmpty(existingDataClassificationNode, fieldName),
                            fieldIdPrefix,
                            fieldName,
                            caseField);
                    } else if (caseFieldType.equalsIgnoreCase(COLLECTION)) {
                        found = true;
                        deduceClassificationForCollectionType(dataNode,
                            getExistingDataClassificationNodeOrEmpty(existingDataClassificationNode, fieldName),
                            fieldIdPrefix,
                            fieldName,
                            caseField);
                    } else {
                        found = true;
                        deduceClassificationForSimpleType((ObjectNode) dataNode,
                            getExistingDataClassificationNodeOrEmpty(existingDataClassificationNode, fieldName),
                            fieldName,
                            caseField);
                    }
                }
            }

            if (!found) {
                ((ObjectNode) dataNode).put(fieldName, DEFAULT_CLASSIFICATION);
            }
        }
    }

    private JsonNode getExistingDataClassificationNodeOrEmpty(JsonNode existingDataClassificationNode,
                                                              String fieldName) {
        return existingDataClassificationNode.has(fieldName) ? existingDataClassificationNode.get(fieldName)
            : JSON_NODE_FACTORY.objectNode();
    }

    private JsonNode getExistingDataClassificationFromArrayOrEmpty(JsonNode existingDataClassificationArray,
                                                                   JsonNode field) {
        if (!existingDataClassificationArray.has(VALUE)) {
            return JSON_NODE_FACTORY.objectNode();
        }
        Iterator<JsonNode> iterator = existingDataClassificationArray.get(VALUE).iterator();
        JsonNode dataClassificationForData = getDataClassificationForData(field,
            iterator);
        return dataClassificationForData.has(VALUE) ? dataClassificationForData.get(VALUE)
            : JSON_NODE_FACTORY.objectNode();
    }

    private void deduceClassificationForSimpleType(ObjectNode dataNode,
                                                   JsonNode existingDataClassificationNode,
                                                   String fieldName,
                                                   CaseFieldDefinition caseField) {
        dataNode.put(fieldName, existingDataClassificationNode.equals(JSON_NODE_FACTORY.objectNode())
            ? getClassificationFromCaseFieldOrDefault(caseField) : existingDataClassificationNode.textValue());
    }

    private String getClassificationFromCaseFieldOrDefault(CaseFieldDefinition caseField) {
        return ofNullable(caseField.getSecurityLabel()).orElse(DEFAULT_CLASSIFICATION);
    }

    private void deduceClassificationForCollectionType(JsonNode dataNode,
                                                       JsonNode existingDataClassificationNode,
                                                       String fieldIdPrefix,
                                                       String fieldName,
                                                       CaseFieldDefinition caseField) {
        final JsonNode fieldNode = dataNode.get(fieldName);
        if (null != fieldNode && fieldNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) fieldNode;
            final FieldTypeDefinition collectionFieldType =
                caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition();
            for (JsonNode field : arrayNode) {

                final JsonNode itemClassification = getExistingDataClassificationFromArrayOrEmpty(
                    existingDataClassificationNode,
                    field);

                if (COMPLEX.equalsIgnoreCase(collectionFieldType.getType())) {
                    deduceDefaultClassifications(
                        field.get(VALUE),
                        // get value of the field with given ID
                        itemClassification,
                        caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields(),
                        fieldIdPrefix + fieldName + FIELD_SEPARATOR);
                } else {
                    final ObjectNode simpleCollectionItemNode = (ObjectNode) field;
                    // Add `classification` property
                    final String newClassification = itemClassification.isTextual()
                        ? itemClassification.textValue() : getClassificationFromCaseFieldOrDefault(caseField);
                    simpleCollectionItemNode.put(CLASSIFICATION, newClassification);
                    // Remove `value` property
                    simpleCollectionItemNode.remove(VALUE);
                }
            }
        }
        ObjectNode valueNode = JSON_NODE_FACTORY.objectNode();
        deduceClassificationForSimpleType(valueNode,
            ofNullable(existingDataClassificationNode.get(CLASSIFICATION)).orElse(JSON_NODE_FACTORY.objectNode()),
            CLASSIFICATION,
            caseField);
        valueNode.set(VALUE, fieldNode);
        ((ObjectNode) dataNode).set(fieldName, valueNode);
    }

    private void deduceClassificationForComplexType(JsonNode dataNode,
                                                    JsonNode existingDataClassificationNode,
                                                    String fieldIdPrefix,
                                                    String fieldName,
                                                    CaseFieldDefinition caseField) {
        deduceDefaultClassifications(
            dataNode.get(fieldName),
            getExistingDataClassificationNodeOrEmpty(existingDataClassificationNode, VALUE),
            caseField.getFieldTypeDefinition().getComplexFields(),
            fieldIdPrefix + fieldName + FIELD_SEPARATOR);
        ObjectNode valueNode = JSON_NODE_FACTORY.objectNode();
        deduceClassificationForSimpleType(valueNode,
            ofNullable(existingDataClassificationNode.get(CLASSIFICATION)).orElse(JSON_NODE_FACTORY.objectNode()),
            CLASSIFICATION,
            caseField);
        valueNode.set(VALUE, dataNode.get(fieldName));
        ((ObjectNode) dataNode).set(fieldName, valueNode);
    }

    public Map<String, JsonNode> cloneDataMap(final Map<String, JsonNode> source) {
        final Map<String, JsonNode> clone = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : source.entrySet()) {
            clone.put(entry.getKey(), entry.getValue().deepCopy());
        }

        return clone;
    }

    private JsonNode cloneAndConvertDataMap(final Map<String, JsonNode> source) {
        if (source == null) {
            return MAPPER.createObjectNode();
        }
        final Map<String, JsonNode> result = cloneDataMap(source);
        return JacksonUtils.convertValueJsonNode(result);
    }
}
