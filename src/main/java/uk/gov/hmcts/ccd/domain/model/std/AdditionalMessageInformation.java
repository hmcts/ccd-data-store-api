package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlock;

import java.util.Map;

@SuppressWarnings("checkstyle:SummaryJavadoc") // Javadoc predates checkstyle implementation in module
public class AdditionalMessageInformation {
    @JsonProperty("Data")
    private Map<String, JsonNode> data;

    @JsonProperty("Definition")
    private Map<String, DefinitionBlock> definition;

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public Map<String, DefinitionBlock> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, DefinitionBlock> definition) {
        this.definition = definition;
    }

}
