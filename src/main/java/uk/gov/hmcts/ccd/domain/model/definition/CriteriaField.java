package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class CriteriaField implements Serializable {
    private String caseFieldId;
    private String caseFieldPath = null;
    private String label;
    private Integer displayOrder;
    private String role;

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("case_field_element_path")
    public String getCaseFieldPath() {
        return caseFieldPath;
    }

    public void setCaseFieldPath(String caseFieldPath) {
        this.caseFieldPath = caseFieldPath;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }
}
