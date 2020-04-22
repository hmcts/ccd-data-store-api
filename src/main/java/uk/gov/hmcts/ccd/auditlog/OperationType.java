package uk.gov.hmcts.ccd.auditlog;

public enum OperationType {
    CREATE_CASE("Create case"),
    UPDATE_CASE("Update case"),
    VIEW_CASE("View case"),
    SEARCH_CASE("Search case"),
    UPDATE_CASE_ACCESS("Update case access"),
    GRANT_CASE_ACCESS("Grant case access"),
    REVOKE_CASE_ACCESS("Revoke case access");

    private final String label;

    OperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
