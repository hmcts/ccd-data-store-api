package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

public enum RoleCategory {
    ROLE_CATEGORY_PROFESSIONAL("PROFESSIONAL"),
    ROLE_CATEGORY_CITIZEN("CITIZEN"),
    ROLE_CATEGORY_JUDICIAL("JUDICIAL"),
    ROLE_CATEGORY_STAFF("STAFF");

    private final String name;

    RoleCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
