package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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
        Map<String, Object> changedKeysAndValues = new HashMap<>();
        if (supplementaryData != null) {
            supplementaryData.keySet().stream().forEach(key -> {
                String newKey = key.replaceAll(Pattern.quote("."), ",");
                changedKeysAndValues.put(newKey, supplementaryData.get(key));
            });
        }
        return changedKeysAndValues;
    }

    @JsonIgnore
    public Map<String, Object> convertRequestData(SupplementaryDataOperation operation) {
        Map<String, Object> supplementaryData = this.requestData.get(operation.getOperationName());
        Map<String, Object> changedKeysAndValues = new HashMap<>();
        if (supplementaryData != null) {
            supplementaryData.entrySet().stream().forEach(entry -> {
                Map<String, Object> nestedMap = generateNestedMap(entry.getKey(), entry.getValue());
                deepMerge(changedKeysAndValues, nestedMap);
            });
        }
        return changedKeysAndValues;
    }

    @JsonIgnore
    public Set<String> getRequestDataKeys() {
        Set<String> keys = new HashSet<>();
        if (this.requestData != null) {
            this.requestData.keySet().forEach(key -> {
                Map<String, Object> operationData = this.requestData.get(key);
                keys.addAll(operationData.keySet());
            });
        }
        return keys;
    }

    private Map<String, Object> generateNestedMap(String path, Object value) {
        final int indexOfDot = path.indexOf('.');
        if (indexOfDot == -1) {
            Map<String, Object> tmpMap = new HashMap<>();
            tmpMap.put(path, value);
            return tmpMap;
        } else {
            Map<String, Object> tmpMap = new HashMap<>();
            tmpMap.put(path.split(Pattern.quote("."))[0],
                generateNestedMap(path.substring(indexOfDot + 1), value));
            return tmpMap;
        }
    }

    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value2 = entry.getValue();
            String key = entry.getKey();
            if (target.containsKey(key)) {
                Object value1 = target.get(key);
                if (value1 instanceof Map && value2 instanceof Map) {
                    deepMerge((Map<String, Object>) value1, (Map<String, Object>) value2);
                } else {
                    target.put(key, value2);
                }
            } else {
                target.put(key, value2);
            }
        }
    }

}
