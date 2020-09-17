package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class CallbackResponse {

    private static final String CALLBACK_RESPONSE_KEY_STATE = "state";
    @ApiModelProperty("Case data as defined in case type definition. See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;
    @JsonProperty("data_classification")
    @ApiModelProperty("Same structure as `data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) "
        + "as field's value.")
    private Map<String, JsonNode> dataClassification;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("significant_item")
    private SignificantItem significantItem;
    private String state;

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

    public Map<String, JsonNode> getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        this.dataClassification = dataClassification;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public SignificantItem getSignificantItem() {
        return significantItem;
    }

    public void setSignificantItem(SignificantItem significantItem) {
        this.significantItem = significantItem;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private Optional<String> filterCaseState(final Map<String, JsonNode> data) {
        final Optional<JsonNode> jsonNode = ofNullable(data.get(CALLBACK_RESPONSE_KEY_STATE));
        jsonNode.ifPresent(value -> data.remove(CALLBACK_RESPONSE_KEY_STATE));
        return jsonNode.flatMap(value -> value.isTextual() ? Optional.of(value.textValue()) : Optional.empty());
    }

    public void updateCallbackStateBasedOnPriority() {
        if (this.getData() != null) {
            final Optional<String> dataCaseState = filterCaseState(this.getData());
            if (Strings.isNullOrEmpty(this.getState()) && dataCaseState.isPresent()) {
                this.setState(dataCaseState.get());
            }
        }
    }
}
