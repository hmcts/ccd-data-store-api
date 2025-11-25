package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

@Schema
public class CaseTypeTabField implements Serializable, CommonDCPModel {
    private CaseFieldDefinition caseFieldDefinition = null;
    private Integer displayOrder = null;
    private String showCondition = null;
    private String displayContextParameter;

    @Schema
    @JsonProperty("case_field")
    public CaseFieldDefinition getCaseFieldDefinition() {
        return caseFieldDefinition;
    }

    public void setCaseFieldDefinition(final CaseFieldDefinition caseFieldDefinition) {
        this.caseFieldDefinition = caseFieldDefinition;
    }

    @Schema
    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Schema
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @Schema
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
