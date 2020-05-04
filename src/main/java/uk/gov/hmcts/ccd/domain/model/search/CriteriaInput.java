package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

public class CriteriaInput {
    private String label;
    private int order;
    private Field field;
    private String role;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public String buildCaseFieldId() {
        if (this.field != null) {
            return StringUtils.isEmpty(this.field.getElementPath()) ? this.field.getId() :
                this.field.getId() + '.' + this.field.getElementPath();
        }
        return null;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
