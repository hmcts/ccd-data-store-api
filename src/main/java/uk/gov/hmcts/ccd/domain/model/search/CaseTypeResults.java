package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CaseTypeResults {

    @JsonProperty("case_type_id")
    private String caseTypeId;
    private long total;

    public CaseTypeResults(String caseTypeId, long total) {

        this.caseTypeId = caseTypeId;
        this.total = total;
    }

    public CaseTypeResults() {
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public long getTotal() {
        return total;
    }
}
