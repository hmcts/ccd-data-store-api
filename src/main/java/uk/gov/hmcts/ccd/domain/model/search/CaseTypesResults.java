package uk.gov.hmcts.ccd.domain.model.search;

public class CaseTypesResults {

    private final String caseType;
    private final long total;

    public CaseTypesResults(String caseFieldId, long numberOfMatchedCases) {

        this.caseType = caseFieldId;
        this.total = numberOfMatchedCases;
    }

    public String getCaseType() {
        return caseType;
    }

    public long getTotal() {
        return total;
    }
}
