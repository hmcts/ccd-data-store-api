package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@SuppressWarnings("checkstyle:SummaryJavadoc") // Javadoc predates checkstyle implementation in module
public class AdditionalMessageInformation {
    @JsonProperty("Data")
    private Map<String, Object> data;

    @JsonProperty("Definition")
    private Map<String, JsonNode> definition;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, JsonNode> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, JsonNode> definition) {
        this.definition = definition;
    }

}
