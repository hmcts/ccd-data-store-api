package uk.gov.hmcts.ccd.auditlog;

public enum AuditOperationType {
    CREATE_CASE_FOR_CASEWORKER("Create case for Caseworker"),
    CREATE_CASE_EVENT_FOR_CASEWORKER("Create case event for Caseworker"),
    GET_CASE_DOCUMENT_FOR_CASEWORKER("Get case document for Caseworker"),
    FIND_CASE_FOR_CASEWORKER("Case Accessed by Caseworker"),
    START_EVENT_FOR_CASEWORKER("Start event for Caseworker"),
    FIND_EVENT_FOR_CASEWORKER("Find event for Caseworker"),
    START_CASE_FOR_CASEWORKER("Start case for Caseworker"),
    SEARCH_CASE_FOR_CASEWORKER("Search case for Caseworker"),
    SEARCH_CASE_METADATA_FOR_CASEWORKER("Search case metadata for Caseworker"),
    SAVE_DRAFT_FOR_CASEWORKER("Save draft for Caseworker"),
    UPDATE_DRAFT_FOR_CASEWORKER("Update draft for Caseworker"),
    FIND_DRAFT_FOR_CASEWORKER("Find draft for Caseworker"),
    DELETE_DRAFT_FOR_CASEWORKER("Delete draft for Caseworker"),

    CREATE_CASE_FOR_CITIZEN("Create case for Citizen"),
    CREATE_CASE_EVENT_FOR_CITIZEN("Create case event for Citizen"),
    FIND_CASE_FOR_CITIZEN("Case Accessed by Citizen"),
    START_EVENT_FOR_CITIZEN("Start event for Citizen"),
    START_CASE_FOR_CITIZEN("Start case for Citizen"),
    SEARCH_CASE_FOR_CITIZEN("Search case for Citizen"),
    SEARCH_CASE_METADATA_FOR_CITIZEN("Search case metadata for Citizen"),

    UPDATE_CASE("Update case"),
    CASE_ACCESSED("Case Accessed"),
    SEARCH_CASE("Search case"),
    VALIDATE_CASE("Validate case"),
    UPDATE_CASE_ACCESS("Update case access permissions"),
    GRANT_CASE_ACCESS("Grant case access permissions"),
    ADD_CASE_ASSIGNED_USER_ROLES("Add Case-Assigned Users and Roles"),
    REMOVE_CASE_ASSIGNED_USER_ROLES("Remove Case-Assigned Users and Roles"),
    GET_CASE_ASSIGNED_USER_ROLES("Get Case-Assigned Users and Roles"),
    REVOKE_CASE_ACCESS("Revoke case access permissions"),
    VIEW_CASE_HISTORY("View case history"),
    FIND_USER_ACCESSIBLE_CASES("Find cases that can be accessed by the user"),
    GET_PRINTABLE_DOCUMENTS("Get printable documents");

    private final String label;

    AuditOperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
