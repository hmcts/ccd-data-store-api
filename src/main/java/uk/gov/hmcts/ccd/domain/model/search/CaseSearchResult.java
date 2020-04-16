package uk.gov.hmcts.ccd.domain.model.search;

import java.util.List;

import static java.util.Collections.emptyList;

import uk.gov.hmcts.ccd.domain.CaseDetails;

public class CaseSearchResult {

    public static final CaseSearchResult EMPTY = new CaseSearchResult(0L, emptyList());

    private Long total;
    private List<CaseDetails> cases;

    public CaseSearchResult() {
    }

    public CaseSearchResult(Long total, List<CaseDetails> cases) {
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
