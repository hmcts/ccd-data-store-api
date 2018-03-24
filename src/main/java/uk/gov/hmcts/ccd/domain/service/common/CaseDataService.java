package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class CaseDataService {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final String COMPLEX_TYPE = "Complex";
    private static final String COLLECTION_TYPE = "Collection";
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";
    private static final String DEFAULT_CLASSIFICATION = "";
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";

    public Map<String, JsonNode> getDefaultSecurityClassifications(final CaseType caseType,
                                                                   final Map<String, JsonNode> caseData) {
        final JsonNode dataClassification = cloneAndConvertDataMap(caseData);
        deduceDefaultClassifications(dataClassification, caseType.getCaseFields(), EMPTY_STRING);
        return MAPPER.convertValue(dataClassification, STRING_JSON_MAP);
    }

    private void deduceDefaultClassifications(final JsonNode dataNode,
                                              final List<CaseField> caseFieldDefinitions,
                                              final String fieldIdPrefix) {
        final Iterator<String> fieldNames = dataNode.fieldNames();
        while (fieldNames.hasNext()) {
            Boolean found = false;
            final String fieldName = fieldNames.next();
            for (CaseField caseField : caseFieldDefinitions) {
                if (caseField.getId().equalsIgnoreCase(fieldName)) {
                    final String caseFieldType = caseField.getFieldType().getType();
                    if (caseFieldType.equalsIgnoreCase(COMPLEX_TYPE)) {
                        found = true;
                        deduceClassificationForComplexType(dataNode, fieldIdPrefix, fieldName, caseField);
                    } else if (caseFieldType.equalsIgnoreCase(COLLECTION_TYPE)) {
                        found = true;
                        deduceClassificationForCollectionType(dataNode, fieldIdPrefix, fieldName, caseField);
                    } else {
                        found = true;
                        deduceClassificationForSimpleType((ObjectNode) dataNode, fieldName, caseField);
                    }
                }
            }

            if (!found) {
                ((ObjectNode) dataNode).put(fieldName, DEFAULT_CLASSIFICATION);
            }
        }
    }

    private void deduceClassificationForSimpleType(ObjectNode dataNode, String fieldName, CaseField caseField) {
        dataNode.put(fieldName, caseField.getSecurityLabel() == null
            ? DEFAULT_CLASSIFICATION : caseField.getSecurityLabel());
    }

    private void deduceClassificationForCollectionType(JsonNode dataNode, String fieldIdPrefix, String fieldName, CaseField caseField) {
        final JsonNode fieldNode = dataNode.get(fieldName);
        if (null != fieldNode && fieldNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) fieldNode;
            final FieldType collectionFieldType = caseField.getFieldType().getCollectionFieldType();
            for (JsonNode field : arrayNode) {
                if (COMPLEX_TYPE.equalsIgnoreCase(collectionFieldType.getType())) {
                    deduceDefaultClassifications(
                        field.get(VALUE),
                        caseField.getFieldType().getCollectionFieldType().getComplexFields(),
                        fieldIdPrefix + fieldName + FIELD_SEPARATOR);
                } else {
                    final ObjectNode simpleCollectionItemNode = (ObjectNode) field;
                    // Add `classification` property
                    deduceClassificationForSimpleType(simpleCollectionItemNode, CLASSIFICATION, caseField);
                    // Remove `value` property
                    simpleCollectionItemNode.remove(VALUE);
                }
            }
        }
        ObjectNode valueNode = JSON_NODE_FACTORY.objectNode();
        valueNode.put(CLASSIFICATION, caseField.getSecurityLabel() == null
            ? DEFAULT_CLASSIFICATION : caseField.getSecurityLabel());
        valueNode.set(VALUE, fieldNode);
        ((ObjectNode) dataNode).set(fieldName, valueNode);
    }

    private void deduceClassificationForComplexType(JsonNode dataNode, String fieldIdPrefix, String fieldName, CaseField caseField) {
        deduceDefaultClassifications(
            dataNode.get(fieldName),
            caseField.getFieldType().getComplexFields(),
            fieldIdPrefix + fieldName + FIELD_SEPARATOR);
        ObjectNode valueNode = JSON_NODE_FACTORY.objectNode();
        valueNode.put(CLASSIFICATION, caseField.getSecurityLabel() == null
            ? DEFAULT_CLASSIFICATION : caseField.getSecurityLabel());
        valueNode.set(VALUE, dataNode.get(fieldName));
        ((ObjectNode) dataNode).set(fieldName, valueNode);
    }

    public Map<String, JsonNode> cloneDataMap(final Map<String, JsonNode> source) {
        final Map<String, JsonNode> clone = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : source.entrySet()) {
            clone.put(entry.getKey(), entry.getValue().deepCopy());
        }

        return  clone;
    }

    private JsonNode cloneAndConvertDataMap(final Map<String, JsonNode> source) {
        if (source == null) {
            return MAPPER.createObjectNode();
        }
        final Map<String, JsonNode> result = cloneDataMap(source);
        return MAPPER.convertValue(result, JsonNode.class);
    }
}
