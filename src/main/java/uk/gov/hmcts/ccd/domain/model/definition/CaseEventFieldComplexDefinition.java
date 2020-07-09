package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import lombok.Builder;

@Builder
public class CaseEventFieldComplexDefinition implements Serializable {

    private String reference;

    private Integer order;

    private String displayContextParameter;

    private String defaultValue;

    public CaseEventFieldComplexDefinition() {
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order,
                                           String defaultValue) {
        this.reference = reference;
        this.order = order;
        this.defaultValue = defaultValue;
    }

    public CaseEventFieldComplexDefinition(String reference,
                                 Integer order,
                                 String displayContextParameter,
                                 String defaultValue) {
        this.reference = reference;
        this.order = order;
        this.displayContextParameter = displayContextParameter;
        this.defaultValue = defaultValue;
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
}
