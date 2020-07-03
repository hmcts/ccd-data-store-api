package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryDataUpdateRequest {

    @JsonIgnore
    private final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    private final PropertiesToJsonConverter propertiesMapper = new PropertiesToJsonConverter();

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
    public String requestedDataToJson(SupplementaryDataOperation operation) {
        Map<String, Object> supplementaryData = this.requestData.get(operation.getOperationName());
        String jsonValue = "";
        if (supplementaryData != null) {
            Properties properties = new Properties();
            supplementaryData.entrySet().stream().forEach(entry -> {
                properties.put(entry.getKey(), entry.getValue());
            });
            jsonValue = propertiesMapper.convertToJson(properties);
        }
        return jsonValue;
    }

    @JsonIgnore
    public String requestedDataJsonOfPath(SupplementaryDataOperation operation, String path) {
        String jsonString = requestedDataToJson(operation);
        DocumentContext context = JsonPath.parse(jsonString);
        Object value = context.read("$." + path, Object.class);
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
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

}
