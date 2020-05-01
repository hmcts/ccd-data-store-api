package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;

@Builder
public class CaseEventFieldComplexDefinition implements Serializable, CommonDCPModel {

    private String reference;

    private Integer order;

    private String displayContextParameter;

    public CaseEventFieldComplexDefinition() {
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order) {
        this.reference = reference;
        this.order = order;
    }

    public CaseEventFieldComplexDefinition(String reference,
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
