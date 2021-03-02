package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    @JsonProperty("supplementary_data_updates")
    private Map<String, Map<String, Object>> requestData;

    @JsonIgnore
    public Map<String, Object> getOperationProperties(SupplementaryDataOperation operation) {
        return this.requestData.getOrDefault(operation.getOperationName(), new HashMap<>());
    }

    @JsonIgnore
    public Set<String> getPropertiesNames() {
        Set<String> allProperties = new HashSet<>();
        if (isValidRequestData()) {
            for (Map<String, Object> propertyNameValuePair : requestData.values()) {
                allProperties.addAll(propertyNameValuePair.keySet());
            }
        }
        return allProperties;
    }

    @JsonIgnore
    public Set<String> getOperations() {
        return this.requestData.keySet();
    }

    @JsonIgnore
    public boolean isValidRequestData() {
        return this.requestData != null && this.requestData.size() > 0;
    }

}
