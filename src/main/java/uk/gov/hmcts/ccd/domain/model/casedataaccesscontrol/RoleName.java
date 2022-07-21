package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.Arrays;
import java.util.Locale;

public enum RoleName {

    ADMIN("hmcts-admin"),
    CTSC("hmcts-ctsc"),
    JUDICIARY("hmcts-judiciary"),
    LEGALOPERATIONS("hmcts-legal-operations");

    private final String roleName;

    RoleName(String roleName) {
        this.roleName = roleName;
    }

    public static boolean isValidRoleName(String name) {
        return Arrays.stream(values())
            .anyMatch(eachRole -> eachRole.roleName.toLowerCase(Locale.ROOT)
                .equals(name.toLowerCase(Locale.ROOT)));
    }

}
