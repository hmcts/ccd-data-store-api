package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;

@Builder
public class CaseEventFieldComplexDefinition implements Serializable, CommonDCPModel {

    private String reference;

    private Integer order;

    private String displayContextParameter;

    private String defaultValue;

    private Boolean retainHiddenValue;

    private Boolean publish;

    private String publishAs;

    public CaseEventFieldComplexDefinition() {
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order,
                                           String defaultValue,
                                           Boolean retainHiddenValue,
                                           Boolean publish,
                                           String publishAs) {
        this.reference = reference;
        this.order = order;
        this.defaultValue = defaultValue;
        this.retainHiddenValue = retainHiddenValue;
        this.publish = publish;
        this.publishAs = publishAs;
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order,
                                           String displayContextParameter,
                                           String defaultValue,
                                           Boolean retainHiddenValue,
                                           Boolean publish,
                                           String publishAs) {

        this.reference = reference;
        this.order = order;
        this.displayContextParameter = displayContextParameter;
        this.defaultValue = defaultValue;
        this.retainHiddenValue = retainHiddenValue;
        this.publish = publish;
        this.publishAs = publishAs;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }

    public String getPublishAs() {
        return publishAs;
    }

    public void setPublishAs(String publishAs) {
        this.publishAs = publishAs;
    }

}
