package uk.gov.hmcts.ccd.domain.model.search;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public class CaseDetailsSearchResult {

    private List<CaseDetails> caseDetails;
    private Long total;

    public CaseDetailsSearchResult() {
    }

    public CaseDetailsSearchResult(List<CaseDetails> caseDetails, Long total) {
        this.caseDetails = caseDetails;
        this.total = total;
    }

    public List<CaseDetails> getCaseDetails() {
        return caseDetails;
    }

    public Long getTotal() {
        return total;
    }
}
