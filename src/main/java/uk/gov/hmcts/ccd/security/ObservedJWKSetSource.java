package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSetSourceWrapper;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Wraps the URL-based source at the bottom of the Nimbus chain and reports whether IDAM actually supplied a
 * usable key set.
 *
 * <p>Nimbus listeners sit above {@code OutageTolerantJWKSetSource}, so they cannot distinguish freshly retrieved
 * keys from stale keys served during an outage. This layer sits below it, and below the retry, so it sees each
 * attempt's own outcome.
 *
 * <p>It wraps the source rather than the resource retriever because the source is where the response is parsed.
 * An HTTP 200 whose body is not a JWK set is a failed retrieval and must not be reported as IDAM having
 * recovered.
 */
class ObservedJWKSetSource<C extends SecurityContext> extends JWKSetSourceWrapper<C> {

    private final JwkSourceTelemetry telemetry;

    ObservedJWKSetSource(JWKSetSource<C> source, JwkSourceTelemetry telemetry) {
        super(source);
        this.telemetry = telemetry;
    }

    @Override
    public JWKSet getJWKSet(JWKSetCacheRefreshEvaluator refreshEvaluator, long currentTime, C context)
        throws KeySourceException {

        JWKSet jwkSet = getSource().getJWKSet(refreshEvaluator, currentTime, context);
        telemetry.retrievalSucceeded();
        return jwkSet;
    }
}
