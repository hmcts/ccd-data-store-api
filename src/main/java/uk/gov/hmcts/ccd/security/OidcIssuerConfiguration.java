package uk.gov.hmcts.ccd.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class OidcIssuerConfiguration {

    private OidcIssuerConfiguration() {
    }

    public static Set<String> allowedIssuers(String primaryIssuer, String configuredAllowedIssuers) {
        LinkedHashSet<String> issuers = new LinkedHashSet<>();
        addIssuer(issuers, primaryIssuer);

        if (configuredAllowedIssuers != null) {
            Arrays.stream(configuredAllowedIssuers.split(","))
                .forEach(issuer -> addIssuer(issuers, issuer));
        }

        if (issuers.isEmpty()) {
            throw new IllegalStateException("At least one OIDC issuer must be configured");
        }

        return Collections.unmodifiableSet(issuers);
    }

    private static void addIssuer(Set<String> issuers, String issuer) {
        if (issuer != null && !issuer.trim().isEmpty()) {
            issuers.add(issuer.trim());
        }
    }
}
