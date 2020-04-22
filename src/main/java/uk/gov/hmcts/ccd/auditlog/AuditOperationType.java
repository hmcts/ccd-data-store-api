package uk.gov.hmcts.ccd.auditlog;

public enum AuditOperationType {
    CREATE_CASE("Create case"),
    UPDATE_CASE("Update case"),
    VIEW_CASE("View case"),
    SEARCH_CASE("Search case"),
    UPDATE_CASE_ACCESS("Update case access"),
    VIEW_CASE_HISTORY("View case history");

    private final String label;

    AuditOperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
