package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SupplementaryDataProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> accessLeafNodes(Map<String, Object> supplementaryData) {
        Map<String, Object> leafNodePathsAndValues = new HashMap<>();
        if (supplementaryData != null && supplementaryData.size() > 0) {
            JsonNode root = objectMapper.valueToTree(supplementaryData);
            processNode(root, new StringBuilder(""), leafNodePathsAndValues);
        }
        return leafNodePathsAndValues;
    }

    private Object getValue(JsonNode jsonNode) {
        if (jsonNode.isValueNode()) {
            if (jsonNode.isInt()) {
                return jsonNode.intValue();
            } else if (jsonNode.isBoolean()) {
                return jsonNode.booleanValue();
            } else {
                return jsonNode.textValue();
            }
        }
        return null;
    }

    private void processNode(JsonNode jsonNode, StringBuilder path, Map<String, Object> leafNodePathsAndValues) {
        if (jsonNode.isValueNode()) {
            String pathToLeaf = path.toString();
            pathToLeaf = pathToLeaf.endsWith(",") ? pathToLeaf.substring(0, pathToLeaf.length() - 1) : pathToLeaf;
            leafNodePathsAndValues.put(pathToLeaf, getValue(jsonNode));
        } else if (jsonNode.isObject()) {
            iterateNode(jsonNode, path, leafNodePathsAndValues);
        }
    }

    private void iterateNode(JsonNode node, StringBuilder path, Map<String, Object> leafNodePathsAndValues) {
        Iterator<Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> jsonField = fields.next();
            StringBuilder childPath = new StringBuilder(path.toString()).append(jsonField.getKey() + ",");
            processNode(jsonField.getValue(), childPath, leafNodePathsAndValues);
        }
    }

}
