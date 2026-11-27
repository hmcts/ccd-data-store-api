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

    @Test
    void shouldTrimAndDeduplicateConfiguredIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(
            " " + PUBLIC_IDAM_ISSUER + ", " + FORGEROCK_ISSUER + ", " + FORGEROCK_ISSUER + " "))
            .containsExactly(PUBLIC_IDAM_ISSUER, FORGEROCK_ISSUER);
    }

    @Test
    void shouldAcceptAnAllowListWithoutAPrimaryIssuer() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(FORGEROCK_ISSUER))
            .containsExactly(FORGEROCK_ISSUER);
    }

    @ParameterizedTest
    @MethodSource("allowedIssuerListsWithEmptyEntries")
    void shouldIgnoreEmptyEntriesInConfiguredAllowedIssuerList(String configuredAllowedIssuers,
                                                              String[] expectedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(configuredAllowedIssuers))
            .containsExactly(expectedIssuers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", ",", " , , "})
    void shouldRejectAnEmptyAllowList(String configuredAllowedIssuers) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(configuredAllowedIssuers))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("idam.security.allowed-issuers must contain at least one issuer");
    }

    @Test
    void shouldReturnAnImmutableAllowList() {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(FORGEROCK_ISSUER).add(PUBLIC_IDAM_ISSUER))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Stream<Arguments> allowedIssuerListsWithEmptyEntries() {
        return Stream.of(
            Arguments.of(
                ", " + FORGEROCK_ISSUER,
                new String[]{FORGEROCK_ISSUER}
            ),
            Arguments.of(
                FORGEROCK_ISSUER + ",",
                new String[]{FORGEROCK_ISSUER}
            ),
            Arguments.of(
                FORGEROCK_ISSUER + ",," + LEGACY_FORGEROCK_ISSUER,
                new String[]{FORGEROCK_ISSUER, LEGACY_FORGEROCK_ISSUER}
            )
        );
    }
}
