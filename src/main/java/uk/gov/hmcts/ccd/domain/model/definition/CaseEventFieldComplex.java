package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;

import java.io.Serializable;

@Builder
public class CaseEventFieldComplex implements Serializable {

    private String reference;

    private Integer order;

    private String displayContextParameter;

    public CaseEventFieldComplex() {
    }

    public CaseEventFieldComplex(String reference,
                                  Integer order) {
        this.reference = reference;
        this.order = order;
    }

    public CaseEventFieldComplex(String reference,
                                 Integer order,
                                 String displayContextParameter) {
        this.reference = reference;
        this.order = order;
        this.displayContextParameter = displayContextParameter;
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
}
