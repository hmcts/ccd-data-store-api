package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class WizardPageField implements Serializable {

    private String caseFieldId = null;
    private Integer order = null;
    private Integer pageColumnNumber;
    private String displayContext;
    private List<WizardPageComplexFieldOverride> complexFieldOverrides = new ArrayList<>();

    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    @JsonProperty("order")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @JsonProperty("page_column_no")
    public Integer getPageColumnNumber() {
        return pageColumnNumber;
    }

    public void setPageColumnNumber(Integer number) {
        this.pageColumnNumber = number;
    }

    @JsonProperty("display_context")
    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @JsonProperty("complex_field_overrides")
    public List<WizardPageComplexFieldOverride> getComplexFieldOverrides() {
        return complexFieldOverrides;
    }

    public void setComplexFieldOverrides(List<WizardPageComplexFieldOverride> complexFieldOverrides) {
        this.complexFieldOverrides = complexFieldOverrides;
    }
}
