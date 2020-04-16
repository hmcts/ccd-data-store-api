package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;

import java.io.Serializable;

@Builder
public class CaseEventFieldComplexDefinition implements Serializable {

    private String reference;

    private Integer order;

    public CaseEventFieldComplexDefinition() {
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order) {
        this.reference = reference;
        this.order = order;
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

}
