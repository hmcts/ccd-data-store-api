package uk.gov.hmcts.ccd.auditlog;

public enum AuditOperationType {
    CREATE_CASE("Create case"),
    UPDATE_CASE("Update case"),
    CASE_ACCESSED("Case Accessed"),
    SEARCH_CASE("Search case"),
    UPDATE_CASE_ACCESS("Update case access permissions"),
    GRANT_CASE_ACCESS("Grant case access permissions"),
    REVOKE_CASE_ACCESS("Revoke case access permissions"),
    VIEW_CASE_HISTORY("View case history");

    private final String label;

    AuditOperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
