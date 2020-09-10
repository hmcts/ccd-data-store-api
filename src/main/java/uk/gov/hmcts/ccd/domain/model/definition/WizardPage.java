package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApiModel(description = "")
public class WizardPage implements Serializable {

    private String id = null;
    private String label = null;
    private Integer order = null;
    private List<WizardPageField> wizardPageFields = new ArrayList<>();
    private String showCondition;
    private String callBackURLMidEvent;
    private List<Integer> retriesTimeoutMidEvent;

    @ApiModelProperty(value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("order")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("wizard_page_fields")
    public List<WizardPageField> getWizardPageFields() {
        return wizardPageFields;
    }

    public void setWizardPageFields(List<WizardPageField> wizardPageFields) {
        this.wizardPageFields = wizardPageFields;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("callback_url_mid_event")
    public String getCallBackURLMidEvent() {
        return callBackURLMidEvent;
    }

    public void setCallBackURLMidEvent(String callBackURLMidEvent) {
        this.callBackURLMidEvent = callBackURLMidEvent;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("retries_timeout_mid_event")
    public List<Integer> getRetriesTimeoutMidEvent() {
        return retriesTimeoutMidEvent == null ? Collections.emptyList() : retriesTimeoutMidEvent;
    }

    public void setRetriesTimeoutMidEvent(List<Integer> retriesTimeoutMidEvent) {
        this.retriesTimeoutMidEvent = retriesTimeoutMidEvent;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @JsonIgnore
    public Set<String> getWizardPageFieldNames() {
        Set<String> wizardPageFields = this.getWizardPageFields()
            .stream()
            .map(WizardPageField::getCaseFieldId)
            .collect(Collectors.toSet());

        Set<String> complexFieldOverRides = this.getWizardPageFields().stream()
            .map(WizardPageField::getComplexFieldOverrides)
            .flatMap(List::stream)
            .map(WizardPageComplexFieldOverride::getComplexFieldElementId)
            .collect(Collectors.toSet());
        wizardPageFields.addAll(complexFieldOverRides);
        return wizardPageFields;
    }
}
