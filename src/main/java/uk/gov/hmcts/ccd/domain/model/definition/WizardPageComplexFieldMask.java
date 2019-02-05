package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;

import java.io.Serializable;

@Builder
@ApiModel(description = "")
public class WizardPageComplexFieldMask implements Serializable {
    private String complexFieldId;
    private String displayContext;
    private Integer order;
    private String label;
    private String hintText;
    private String showCondition;

    @JsonProperty("complex_field_id")
    public String getComplexFieldId() {
        return complexFieldId;
    }

    public void setComplexFieldId(String complexFieldId) {
        this.complexFieldId = complexFieldId;
    }

    @JsonProperty("display_context")
    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @JsonProperty("order")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }
}
