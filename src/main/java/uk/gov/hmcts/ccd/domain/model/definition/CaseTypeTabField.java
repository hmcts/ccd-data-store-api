package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class CaseTypeTabField implements Serializable {
    private CaseField caseField = null;
    private Integer displayOrder = null;

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
}
