package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

public class CaseEventFieldComplex implements Serializable {

    private String reference;

    private String hint;

    private String label;

    private Integer order;

    private DisplayContext displayContext;

    private String showCondition;

    public CaseEventFieldComplex() {
    }

    public CaseEventFieldComplex(String reference,
                                 String hint,
                                 String label,
                                 Integer order,
                                 DisplayContext displayContext,
                                 String showCondition) {
        this.reference = reference;
        this.hint = hint;
        this.label = label;
        this.order = order;
        this.displayContext = displayContext;
        this.showCondition = showCondition;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public DisplayContext getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(DisplayContext displayContext) {
        this.displayContext = displayContext;
    }

    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }
}
