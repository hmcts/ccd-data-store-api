package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;

@ApiModel(description = "")
public class WizardPageComplexFieldOverride implements Serializable {
    private String complexFieldElementId;
    private String displayContext;
    private Integer order;
    private String label;
    private String hintText;
    private String showCondition;

    @JsonProperty("complex_field_element_id")
    public String getComplexFieldElementId() {
        return complexFieldElementId;
    }

    public void setComplexFieldElementId(String complexFieldElementId) {
        this.complexFieldElementId = complexFieldElementId;
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
