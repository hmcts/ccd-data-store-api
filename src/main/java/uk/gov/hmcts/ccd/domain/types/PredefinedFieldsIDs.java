package uk.gov.hmcts.ccd.domain.types;

/**
 * IDs of fields in predefined complex types.
 */
public enum PredefinedFieldsIDs {

    ORG_POLICY_CASE_ASSIGNED_ROLE("OrgPolicyCaseAssignedRole");

    private String id;

    PredefinedFieldsIDs(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
