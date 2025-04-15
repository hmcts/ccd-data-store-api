package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Schema
public class WorkbasketInputFieldsDefinition implements Serializable, CommonDCPModel {

    private String caseTypeId = null;
    private List<WorkbasketInputField> fields = new ArrayList<>();
    private String displayContextParameter = null;

    @Schema
    @JsonProperty("case_type_id")
    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    @Schema
    @JsonProperty("fields")
    public List<WorkbasketInputField> getFields() {
        return fields;
    }

    public void setFields(List<WorkbasketInputField> fields) {
        this.fields = fields;
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
