package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResultField {
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("case_field_id")
    private String caseFieldId;
    private String label;
    @JsonProperty("order")
    private Integer displayOrder;

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
