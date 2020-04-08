package uk.gov.hmcts.ccd.auditlog;

public enum OperationType {
    CREATE_CASE("Create case"),
    UPDATE_CASE("Update case"),
    VIEW_CASE("View case"),
    SEARCH_CASE("Search case"),
    GRANT_CASE_ACCESS("Grant case access"),
    REVOKE_CASE_ACCESS("Revoke case access"),
    GRANT_CASE_ROLE("Grant case role"),
    REVOKE_CASE_ROLE("Revoke case role");

    private final String label;

    private OperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
