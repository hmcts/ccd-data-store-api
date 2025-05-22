package uk.gov.hmcts.ccd.domain.model.definition;

import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;

@ToString
@Schema
public class CaseEventFieldDefinition implements Serializable, CommonDCPModel, Copyable<CaseEventFieldDefinition> {

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
    private String defaultValue;
    private Boolean nullifyByDefault;

    @Schema(required = true, description = "Foreign key to CaseField.id")
    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    @Schema(description = "whether this field is optional, mandatory or read only for this event")
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

    @Schema(description = "contain names of fields for list or table")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    @Schema(description = "Show Condition expression for this field")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @Schema(description = "Show Summary Change Option")
    @JsonProperty("show_summary_change_option")
    public Boolean getShowSummaryChangeOption() {
        return showSummaryChangeOption;
    }

    public void setShowSummaryChangeOption(final Boolean showSummaryChangeOption) {
        this.showSummaryChangeOption = showSummaryChangeOption;
    }

    @Schema(description = "Show Summary Content Option")
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
    @Schema
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
    @Schema
    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @Schema
    @JsonProperty("case_fields_complex")
    public List<CaseEventFieldComplexDefinition> getCaseEventFieldComplexDefinitions() {
        return caseEventFieldComplexDefinitions;
    }

    public void setCaseEventFieldComplexDefinitions(List<CaseEventFieldComplexDefinition> eventComplexTypeEntities) {
        this.caseEventFieldComplexDefinitions = eventComplexTypeEntities;
    }

    @Schema(description = "whether this field is data should be retained, dependant on show_condition being"
            + " populated")
    @JsonProperty("retain_hidden_value")
    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

    @Schema(description = "whether this field is data should be published")
    @JsonProperty("publish")
    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }


    @Schema(description = "Alias for field id if published is set to true")
    @JsonProperty("publish_as")
    public String getPublishAs() {
        return publishAs;
    }

    public void setPublishAs(String publishAs) {
        this.publishAs = publishAs;
    }

    @Schema(description = "Default value for the case field, if no existing value")
    @JsonProperty("default_value")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Schema(description = "Nullify By Default value for the case field")
    @JsonProperty("nullify_by_default")
    public Boolean getNullifyByDefault() {
        return nullifyByDefault;
    }

    public void setNullifyByDefault(Boolean nullifyByDefault) {
        this.nullifyByDefault = nullifyByDefault;
    }

    @JsonIgnore
    @Override
    public CaseEventFieldDefinition createCopy() {
        CaseEventFieldDefinition copy = new CaseEventFieldDefinition();
        copy.setCaseFieldId(this.getCaseFieldId());
        copy.setDisplayContext(this.getDisplayContext());
        copy.setDisplayContextParameter(this.getDisplayContextParameter());
        copy.setShowCondition(this.getShowCondition());
        copy.setShowSummaryChangeOption(this.getShowSummaryChangeOption());
        copy.setShowSummaryContentOption(this.getShowSummaryContentOption());
        copy.setLabel(this.getLabel());
        copy.setHintText(this.getHintText());
        copy.setRetainHiddenValue(this.getRetainHiddenValue());
        copy.setPublish(this.getPublish());
        copy.setPublishAs(this.getPublishAs());
        copy.setCaseEventFieldComplexDefinitions(createCopyList(this.getCaseEventFieldComplexDefinitions()));
        copy.setDefaultValue(this.getDefaultValue());
        copy.setNullifyByDefault(this.getNullifyByDefault());

        return copy;
    }
}
