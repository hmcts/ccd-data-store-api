package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class CaseTypeTabDefinition implements Serializable, CommonDCPModel {

    private String id = null;
    private String label = null;
    @JsonProperty("order")
    private Integer displayOrder = null;
    @JsonProperty("tab_fields")
    private List<CaseTypeTabField> tabFields = new ArrayList<>();
    @JsonProperty("show_condition")
    private String showCondition;
    private String role;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<CaseTypeTabField> getTabFields() {
        return tabFields;
    }

    public void setTabFields(List<CaseTypeTabField> tabFields) {
        this.tabFields = tabFields;
    }

    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
