package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CallbackResponse {
    private Map<String, JsonNode> data;
    private List<String> errors;
    private List<String> warnings;

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public List<String> getErrors() {
        if (errors == null) {
            errors = Collections.emptyList();
        }
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        if (warnings == null) {
            warnings = Collections.emptyList();
        }
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
