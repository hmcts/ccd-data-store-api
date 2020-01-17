package uk.gov.hmcts.ccd.infrastructure.user;

import java.util.Set;

public class UserAuthorisation {

    private final String id;
    private final AccessLevel accessLevel;
    private final Set<String> roles;

    public UserAuthorisation(String id, AccessLevel accessLevel, Set<String> roles) {
        this.id = id;
        this.accessLevel = accessLevel;
        this.roles = roles;
    }

    public String getUserId() {
        return id;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public Boolean hasJurisdictionRole(String jurisdictionId) {
        final String jurisdictionRole = "caseworker-" + jurisdictionId;
        return roles.stream().anyMatch(jurisdictionRole::equalsIgnoreCase);
    }

    public Set<String> getRoles() {
        return roles;
    }

    /**
     * User access level, as inferred from the user roles.
     * <ul>
     *     <li>ALL: Can see all cases within the limits defined by CRUD</li>
     *     <li>GRANTED: In addition to CRUD constraints, users can only
     *     see cases to which they were explicitly granted access</li>
     * </ul>
     */
    public enum AccessLevel {
        ALL, GRANTED
    }
}
