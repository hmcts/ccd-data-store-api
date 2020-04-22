package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

@ApiModel(description = "")
public class CaseTypeTabField implements Serializable, CommonDCPModel {
    private CaseField caseField = null;
    private Integer displayOrder = null;
    private String showCondition = null;
    private String displayContextParameter;

    @ApiModelProperty(value = "")
    @JsonProperty("case_field")
    public CaseField getCaseField() {
        return caseField;
    }

    public void setCaseField(final CaseField caseField) {
        this.caseField = caseField;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
