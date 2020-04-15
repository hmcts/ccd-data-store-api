package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApiModel(description = "")
public class WizardPageField implements Serializable {

    private String caseFieldId = null;
    private Integer order = null;
    private Integer pageColumnNumber;
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

    @JsonProperty("complex_field_overrides")
    public List<WizardPageComplexFieldOverride> getComplexFieldOverrides() {
        return complexFieldOverrides;
    }

    public void setComplexFieldOverrides(List<WizardPageComplexFieldOverride> complexFieldOverrides) {
        this.complexFieldOverrides = complexFieldOverrides;
    }

    public Optional<WizardPageComplexFieldOverride> getComplexFieldOverride(String fieldPath) {
        return getComplexFieldOverrides().stream()
            .filter(override -> fieldPath.equals(override.getComplexFieldElementId()))
            .findAny();
    }
}
