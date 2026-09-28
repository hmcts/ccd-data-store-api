package uk.gov.hmcts.ccd.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIssuerConfigurationTest {

    private static final String PUBLIC_IDAM_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String FORGEROCK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443"
            + "/openam/oauth2/realms/root/realms/hmcts";
    private static final String LEGACY_FORGEROCK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/hmcts";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldOnlyHavePrimaryIssuerWhenAllowedIssuersMissing(String configuredAllowedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PUBLIC_IDAM_ISSUER, configuredAllowedIssuers))
            .containsExactly(PUBLIC_IDAM_ISSUER);
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(
            OidcIssuerConfiguration.allowedIssuers(
                PUBLIC_IDAM_ISSUER,
                " " + FORGEROCK_ISSUER + ", " + LEGACY_FORGEROCK_ISSUER + " , " + FORGEROCK_ISSUER + " "
            )
        ).containsExactly(PUBLIC_IDAM_ISSUER, FORGEROCK_ISSUER, LEGACY_FORGEROCK_ISSUER);
    }

    @ParameterizedTest
    @MethodSource("allowedIssuerListsWithEmptyEntries")
    void shouldIgnoreEmptyEntriesInConfiguredAllowedIssuerList(String configuredAllowedIssuers,
                                                              String[] expectedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PUBLIC_IDAM_ISSUER, configuredAllowedIssuers))
            .containsExactly(expectedIssuers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankPrimaryIssuerEvenWhenAllowedIssuersAreConfigured(String primaryIssuer) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(primaryIssuer, FORGEROCK_ISSUER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("oidc.issuer must not be blank");
    }

    private static Stream<Arguments> allowedIssuerListsWithEmptyEntries() {
        return Stream.of(
            Arguments.of(
                ", " + FORGEROCK_ISSUER,
                new String[]{PUBLIC_IDAM_ISSUER, FORGEROCK_ISSUER}
            ),
            Arguments.of(
                FORGEROCK_ISSUER + ",",
                new String[]{PUBLIC_IDAM_ISSUER, FORGEROCK_ISSUER}
            ),
            Arguments.of(
                FORGEROCK_ISSUER + ",," + LEGACY_FORGEROCK_ISSUER,
                new String[]{PUBLIC_IDAM_ISSUER, FORGEROCK_ISSUER, LEGACY_FORGEROCK_ISSUER}
            )
        );
    }
}
