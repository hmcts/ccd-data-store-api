package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumerations of reserved global case roles.
 * <ul>
 *  <li>[CREATOR]: Generic author of a cause, automatically assigned at case creation</li>
 * </ul>
 */
public enum GlobalCaseRole {
    CREATOR("[CREATOR]"),
    COLLABORATOR("[COLLABORATOR]");

    private final String role;

    GlobalCaseRole(String role) {
        this.role = role;
    }

    public static Set<String> all() {
        return Arrays.stream(GlobalCaseRole.values())
                     .map(GlobalCaseRole::getRole)
                     .collect(Collectors.toSet());
    }

    /**
     * Get Role.
     *
     * @return String representation of the role as stored in database
     */
    public String getRole() {
        return role;
    }
}
