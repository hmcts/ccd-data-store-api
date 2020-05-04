package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
@ApiModel(description = "")
public class SearchInputDefinition implements Serializable {

    private String caseTypeId = null;
    private List<SearchInputField> fields = new ArrayList<>();
    private String displayContextParameter = null;

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("case_type_id")
    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("fields")
    public List<SearchInputField> getFields() {
        return fields;
    }

    public void setFields(List<SearchInputField> fields) {
        this.fields = fields;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

}
