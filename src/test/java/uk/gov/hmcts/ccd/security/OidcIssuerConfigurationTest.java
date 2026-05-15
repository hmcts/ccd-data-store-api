package uk.gov.hmcts.ccd.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIssuerConfigurationTest {

    @Test
    void shouldFallbackToPrimaryIssuerWhenAllowedIssuersUnset() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", null))
            .containsExactly("primary");
    }

    @Test
    void shouldFallbackToPrimaryIssuerWhenAllowedIssuersBlank() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", " "))
            .containsExactly("primary");
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", " secondary, tertiary , secondary "))
            .containsExactly("primary", "secondary", "tertiary");
    }

    @Test
    void shouldRejectEmptyIssuerConfiguration() {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(" ", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("At least one OIDC issuer must be configured");
    }
}
