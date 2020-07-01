package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryDataUpdateRequest {

    @JsonIgnore
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Map<String, Object>> requestData;

    public Optional<Map<String, Object>> getOperationData(SupplementaryDataOperation operation) {
        return Optional.ofNullable(this.requestData.get(operation.getOperationName()));
    }

    @JsonIgnore
    public Map<String, Object> getUpdateOperationProperties(SupplementaryDataOperation operation) {
        Map<String, Object> supplementaryData = this.requestData.get(operation.getOperationName());
        Map<String, Object> propertiesAndValues = new HashMap<>();
        if (supplementaryData != null && supplementaryData.size() > 0) {
            JsonNode root = objectMapper.valueToTree(supplementaryData);
            processNode(root, new StringBuilder(""), propertiesAndValues);
        }
        return propertiesAndValues;
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

    private void processNode(JsonNode jsonNode, StringBuilder path, Map<String, Object> propertiesAndValues) {
        if (jsonNode.isValueNode()) {
            String leafNodePath = getLeafNodePath(path.toString());
            propertiesAndValues.put(leafNodePath, getValue(jsonNode));
        } else if (jsonNode.isObject()) {
            iterateNode(jsonNode, path, propertiesAndValues);
        }
    }

    private String getLeafNodePath(String path) {
        return path.endsWith(",") ? path.substring(0, path.length() - 1) : path;
    }

    private void iterateNode(JsonNode node, StringBuilder path, Map<String, Object> propertiesAndValues) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> jsonField = fields.next();
            StringBuilder childPath = new StringBuilder(path.toString()).append(jsonField.getKey() + ",");
            processNode(jsonField.getValue(), childPath, propertiesAndValues);
        }
    }
}
