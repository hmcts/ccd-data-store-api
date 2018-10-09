package uk.gov.hmcts.ccd.infrastructure.user;

public class UserAuthorisation {

    private final String id;
    private final AccessLevel accessLevel;

    public UserAuthorisation(String id, AccessLevel accessLevel) {
        this.id = id;
        this.accessLevel = accessLevel;
    }

    public String getUserId() {
        return id;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
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
