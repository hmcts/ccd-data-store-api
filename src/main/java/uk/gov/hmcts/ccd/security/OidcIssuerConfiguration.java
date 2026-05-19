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
        issuers.add(requirePrimaryIssuer(primaryIssuer));

        if (configuredAllowedIssuers != null) {
            Arrays.stream(configuredAllowedIssuers.split(","))
                .forEach(issuer -> addIssuer(issuers, issuer));
        }

        return Collections.unmodifiableSet(issuers);
    }

    private static String requirePrimaryIssuer(String primaryIssuer) {
        if (primaryIssuer == null || primaryIssuer.trim().isEmpty()) {
            throw new IllegalStateException("oidc.issuer must not be blank");
        }

        return primaryIssuer.trim();
    }

    private static void addIssuer(Set<String> issuers, String issuer) {
        if (issuer != null && !issuer.trim().isEmpty()) {
            issuers.add(issuer.trim());
        }
    }
}
