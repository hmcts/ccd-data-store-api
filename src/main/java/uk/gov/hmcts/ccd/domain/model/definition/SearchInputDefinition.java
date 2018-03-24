package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class SearchInputDefinition {

    private String caseTypeId = null;
    private List<SearchInputField> fields = new ArrayList<>();

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SearchInputDefinition {");

        sb.append("  caseTypeId: ").append(caseTypeId).append("");
        sb.append("  fields: ").append(fields).append("");
        sb.append("}");
        return sb.toString();
    }
}
