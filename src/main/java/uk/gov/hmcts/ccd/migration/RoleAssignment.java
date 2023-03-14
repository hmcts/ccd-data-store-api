package uk.gov.hmcts.ccd.migration;

public class RoleAssignment {
    String caseReference;
    String userId;
    String roleName;

    public RoleAssignment(String caseReference, String userId, String roleName) {
        this.caseReference = caseReference;
        this.userId = userId;
        this.roleName = roleName;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() {
        return "RoleAssignment{" +
            "caseReference='" + caseReference + '\'' +
            ", userId='" + userId + '\'' +
            ", roleName='" + roleName + '\'' +
            '}';
    }
}
