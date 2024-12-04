package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseAccessServiceUserRoleTest {

    // This pattern is used to restrict the roles that can be granted to external users
    // defined in CaseAccessService#isExternalUser
    private static final Pattern RESTRICT_GRANTED_ROLES_PATTERN
        = Pattern.compile(".+-solicitor$|.+-panelmember$|^citizen(-.*)?$|^letter-holder$|^caseworker-."
        + "+-localAuthority$");

    @Test
    void shouldMatchRestrictedRolesIsExternalUser() {
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-divorce-solicitor").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-civil-solicitor").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-probate-solicitor").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-sscs-solicitor").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-criminal-panelmember").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("citizen").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("citizen-loa1").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("letter-holder").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-family-localAuthority").matches());
        assertTrue(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-publiclaw-localAuthority").matches());
    }

    @Test
    void shouldNotMatchNonRestrictedRolesIsLocalUser() {
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-divorce").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-divorce-financialremedy").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("admin").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-divorce-judge").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("caseworker-divorce-courtadmin-la").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("fpl-caseworker-publiclaw").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("fpl-caseworker-publiclaw-cafcass").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("pui-organisation-manager").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("pui-user-manager").matches());
        assertFalse(RESTRICT_GRANTED_ROLES_PATTERN.matcher("pui-finance-manager").matches());
    }

}
