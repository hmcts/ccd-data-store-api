package uk.gov.hmcts.ccd.data.caseaccess;

/**
 * Enumerations of reserved global case roles:
 * <ul>
 *  <li>[CREATOR]: Generic author of a cause, automatically assigned at case creation</li>
 * </ul>
 */
public enum GlobalCaseRole {
    CREATOR("[CREATOR]");

    private final String role;

    GlobalCaseRole(String role) {
        this.role = role;
    }

    /**
     * @return String representation of the role as stored in database
     */
    public String getRole() {
        return role;
    }
}
