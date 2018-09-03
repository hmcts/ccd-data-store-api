package uk.gov.hmcts.ccd.domain.model.search;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;

public class CaseDetailsSearchResult {

    private List<CaseDetails> cases;
    private Long total;

    public CaseDetailsSearchResult() {
    }

    public CaseDetailsSearchResult(List<CaseDetails> cases, Long total) {
        this.cases = cases;
        this.total = total;
    }

    public List<CaseDetails> getCases() {
        return cases;
    }

    public Long getTotal() {
        return total;
    }
}
