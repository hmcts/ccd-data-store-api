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

    public enum AccessLevel {
        ALL, GRANTED
    }
}
