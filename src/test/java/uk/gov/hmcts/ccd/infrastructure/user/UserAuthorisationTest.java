package uk.gov.hmcts.ccd.infrastructure.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserAuthorisation")
class UserAuthorisationTest {

    private static final String USER_ID = "123";
    private static final AccessLevel ACCESS_LEVEL = AccessLevel.ALL;

    @Nested
    @DisplayName("hasJurisdictionRole")
    class HasJurisdictionRole {
        @Test
        @DisplayName("should return false when user has no roles")
        void shouldBeFalseWhenNoRoles() {
            final UserAuthorisation userAuthorisation = newUserAuthorization();

            assertFalse(userAuthorisation.hasJurisdictionRole("any"));
        }

        @Test
        @DisplayName("should return true when user has base jurisdiction role")
        void shouldBeTrueWhenHasJurisdictionBaseRole() {
            final UserAuthorisation userAuthorisation = newUserAuthorization("caseworker-divorce");

            assertTrue(userAuthorisation.hasJurisdictionRole("divorce"));
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            final UserAuthorisation userAuthorisation = newUserAuthorization("caseworker-divorce");

            assertTrue(userAuthorisation.hasJurisdictionRole("DIVORCE"));
        }

        @Test
        @DisplayName("should return false when user has not base jurisdiction role")
        void shouldBeFalseWhenHasNotJurisdictionBaseRole() {
            final UserAuthorisation userAuthorisation = newUserAuthorization("caseworker",
                                                                             "caseworker-finrem-divorce");

            assertFalse(userAuthorisation.hasJurisdictionRole("divorce"));
        }
    }

    private UserAuthorisation newUserAuthorization(String... roles) {
        return new UserAuthorisation(USER_ID, ACCESS_LEVEL, new HashSet<>(Arrays.asList(roles)));
    }
}
