package uk.gov.hmcts.ccd.domain.service.search.filter;

public enum CaseSearchFilter {
    CASE_STATE("state"),
    SECURITY_CLASSIFICATION("security_classification"),
    CASE_ID("id");

    private final String filterName;

    CaseSearchFilter(String filterName) {
        this.filterName = filterName;
    }

    public String filterName() {
        return filterName;
    }
}
