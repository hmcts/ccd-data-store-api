package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class CaseSearchResult {
    public static final CaseSearchResult EMPTY = new CaseSearchResult(0L, emptyList());

    private Long total;
    private List<CaseDetails> cases;

    @JsonProperty("case_types_results")
    private List<CaseTypeResults> caseTypesResults;

    public CaseSearchResult() {
    }

    public CaseSearchResult(Long total, List<CaseDetails> cases, List<CaseTypeResults> caseTypesResults) {
        this.cases = cases;
        this.total = total;
        this.caseTypesResults = caseTypesResults;
    }

    public CaseSearchResult(Long total, List<CaseDetails> cases) {
        this(total, cases, new ArrayList<>());
    }

    public List<CaseDetails> getCases() {
        return cases;
    }

    public Long getTotal() {
        return total;
    }

    public List<String> getCaseReferences(String caseTypeId) {
        return cases == null
            ? emptyList()
            : cases.stream().filter(c ->
            c.getCaseTypeId().equals(caseTypeId)).map(CaseDetails::getReferenceAsString).collect(toList());
    }

    public List<CaseTypeResults> getCaseTypesResults() {
        return caseTypesResults;
    }
}
