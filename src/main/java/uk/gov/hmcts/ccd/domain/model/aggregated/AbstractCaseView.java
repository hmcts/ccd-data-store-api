package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public abstract class AbstractCaseView {

    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_type")
    private CaseViewType caseType;
    private CaseViewTab[] tabs;
    private List<CaseViewField> metadataFields;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public CaseViewType getCaseType() {
        return caseType;
    }

    public void setCaseType(CaseViewType caseType) {
        this.caseType = caseType;
    }

    public CaseViewTab[] getTabs() {
        return tabs;
    }

    public void setTabs(CaseViewTab[] tabs) {
        this.tabs = tabs;
    }

    public List<CaseViewField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<CaseViewField> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public void addMetadataFields(List<CaseViewField> metadataFields) {
        if (this.metadataFields == null) {
            this.metadataFields = new ArrayList<>();
        }
        this.metadataFields.addAll(metadataFields);
    }
}
