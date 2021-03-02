package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
@ApiModel(description = "")
public class CaseEventFieldDefinition implements Serializable, CommonDCPModel {

    private String caseFieldId = null;
    private String displayContext = null;
    private String displayContextParameter = null;
    private String showCondition = null;
    private Boolean showSummaryChangeOption = null;
    private Integer showSummaryContentOption = null;
    private String label = null;
    private String hintText = null;
    private Boolean retainHiddenValue;
    private Boolean publish;
    private String publishAs;
    private List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions = new ArrayList<>();

    @ApiModelProperty(required = true, value = "Foreign key to CaseField.id")
    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    @ApiModelProperty(value = "whether this field is optional, mandatory or read only for this event")
    @JsonProperty("display_context")
    public String getDisplayContext() {
        return displayContext;
    }

    @JsonIgnore
    public DisplayContext getDisplayContextEnum() {
        return DisplayContext.valueOf(displayContext);
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @ApiModelProperty(value = "contain names of fields for list or table")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    @ApiModelProperty(value = "Show Condition expression for this field")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @ApiModelProperty(value = "Show Summary Change Option")
    @JsonProperty("show_summary_change_option")
    public Boolean getShowSummaryChangeOption() {
        return showSummaryChangeOption;
    }

    public void setShowSummaryChangeOption(final Boolean showSummaryChangeOption) {
        this.showSummaryChangeOption = showSummaryChangeOption;
    }

    @ApiModelProperty(value = "Show Summary Content Option")
    @JsonProperty("show_summary_content_option")
    public Integer getShowSummaryContentOption() {
        return showSummaryContentOption;
    }

    public void setShowSummaryContentOption(Integer showSummaryContentOption) {
        this.showSummaryContentOption = showSummaryContentOption;
    }

    /**
     * event case field label.
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * event case field hint text.
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("case_fields_complex")
    public List<CaseEventFieldComplexDefinition> getCaseEventFieldComplexDefinitions() {
        return caseEventFieldComplexDefinitions;
    }

    public void setCaseEventFieldComplexDefinitions(List<CaseEventFieldComplexDefinition> eventComplexTypeEntities) {
        this.caseEventFieldComplexDefinitions = eventComplexTypeEntities;
    }

    @ApiModelProperty(value = "whether this field is data should be retained, dependant on show_condition being"
            + " populated")
    @JsonProperty("retain_hidden_value")
    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

    @ApiModelProperty(value = "whether this field is data should be published")
    @JsonProperty("publish")
    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }


    @ApiModelProperty(value = "Alias for field id if published is set to true")
    @JsonProperty("publish_as")
    public String getPublishAs() {
        return publishAs;
    }

    public void setPublishAs(String publishAs) {
        this.publishAs = publishAs;
    }

}
