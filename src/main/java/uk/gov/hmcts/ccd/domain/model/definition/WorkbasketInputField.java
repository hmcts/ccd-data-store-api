package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class WorkbasketInputField implements Serializable {

    private String caseFieldId = null;
    private String caseFieldElementPath = null;
    private String label = null;
    private Integer order = null;

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
    public String getCaseFieldElementPath() {
        return caseFieldElementPath;
    }

    public void setCaseFieldElementPath(String caseFieldElementPath) {
        this.caseFieldElementPath = caseFieldElementPath;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("order")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
