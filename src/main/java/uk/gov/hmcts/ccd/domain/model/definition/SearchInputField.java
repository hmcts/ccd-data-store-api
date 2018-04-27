package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@ApiModel(description = "")
public class SearchInputField implements Serializable {

    private String caseFieldId = null;
    private String label = null;
    private Integer displayOrder = null;

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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("caseFieldId", caseFieldId)
            .append("label", label)
            .append("displayOrder", displayOrder)
            .toString();
    }
}
