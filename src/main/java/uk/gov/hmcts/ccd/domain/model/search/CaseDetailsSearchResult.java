package uk.gov.hmcts.ccd.domain.model.search;

import java.util.List;

import static java.util.Collections.emptyList;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public class CaseDetailsSearchResult {

    public static final CaseDetailsSearchResult EMPTY = new CaseDetailsSearchResult(emptyList(), 0L);

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
