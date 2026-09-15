package uk.gov.hmcts.ccd.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Settings used to retrieve the IDAM signing keys (JWK set).
 *
 * <p>These settings are related and some combinations are not valid. Nimbus
 * {@link com.nimbusds.jose.jwk.source.JWKSourceBuilder} rejects some combinations itself,
 * while another combination can lead to the production failure this configuration is intended
 * to prevent.
 *
 * <p>They are therefore checked in {@link #validate()} when the bean is created. This ensures
 * configuration errors are caught at start-up rather than when the first token needs to be
 * verified during an IDAM outage.
 *
 * @param uri                    JWK set endpoint
 * @param connectTimeoutMs       TCP connect timeout for retrieval
 * @param readTimeoutMs          socket read timeout for retrieval
 * @param sizeLimitBytes         maximum accepted response size
 * @param cacheTtlMs             how long a retrieved key set can be used before it needs refreshing
 * @param cacheRefreshTimeoutMs  how long a thread waits for another thread's in-flight retrieval
 * @param refreshAheadTimeMs     how far before expiry the background refresh is scheduled
 * @param rateLimitMinIntervalMs minimum interval between retrievals reaching IDAM
 * @param outageToleranceMs      how long the last retrieved key set can be used while IDAM is unavailable
 */
@ConfigurationProperties("oidc.jwks")
public record JwksProperties(
    String uri,
    @DefaultValue("2000") int connectTimeoutMs,
    @DefaultValue("5000") int readTimeoutMs,
    @DefaultValue("51200") int sizeLimitBytes,
    @DefaultValue("300000") long cacheTtlMs,
    @DefaultValue("10000") long cacheRefreshTimeoutMs,
    @DefaultValue("60000") long refreshAheadTimeMs,
    @DefaultValue("30000") long rateLimitMinIntervalMs,
    @DefaultValue("21600000") long outageToleranceMs
) {

    public JwksProperties {
        validate(uri, connectTimeoutMs, readTimeoutMs, sizeLimitBytes, cacheTtlMs, cacheRefreshTimeoutMs,
            refreshAheadTimeMs, rateLimitMinIntervalMs, outageToleranceMs);
    }

    public URL url() {
        try {
            return new URI(uri).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid oidc.jwks.uri: " + uri, e);
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void validate(String uri,
                                 int connectTimeoutMs,
                                 int readTimeoutMs,
                                 int sizeLimitBytes,
                                 long cacheTtlMs,
                                 long cacheRefreshTimeoutMs,
                                 long refreshAheadTimeMs,
                                 long rateLimitMinIntervalMs,
                                 long outageToleranceMs) {

        require(uri != null && !uri.isBlank(), "oidc.jwks.uri must be set");
        require(connectTimeoutMs > 0, "oidc.jwks.connect-timeout-ms must be greater than 0, but was %d; "
            + "0 means an infinite timeout, which is what let a single unreachable IDAM stall request threads "
            + "indefinitely", connectTimeoutMs);
        require(readTimeoutMs > 0, "oidc.jwks.read-timeout-ms must be greater than 0, but was %d; "
            + "0 means an infinite timeout, which is what let a single unreachable IDAM stall request threads "
            + "indefinitely", readTimeoutMs);
        require(sizeLimitBytes > 0, "oidc.jwks.size-limit-bytes must be greater than 0, but was %d",
            sizeLimitBytes);
        require(cacheTtlMs > 0, "oidc.jwks.cache-ttl-ms must be greater than 0, but was %d",
            cacheTtlMs);
        require(cacheRefreshTimeoutMs > 0,
            "oidc.jwks.cache-refresh-timeout-ms must be greater than 0, but was %d", cacheRefreshTimeoutMs);
        require(refreshAheadTimeMs > 0,
            "oidc.jwks.refresh-ahead-time-ms must be greater than 0, but was %d", refreshAheadTimeMs);
        require(rateLimitMinIntervalMs > 0,
            "oidc.jwks.rate-limit-min-interval-ms must be greater than 0, but was %d", rateLimitMinIntervalMs);
        require(outageToleranceMs > 0,
            "oidc.jwks.outage-tolerance-ms must be greater than 0, but was %d", outageToleranceMs);

        // The cache waiter must be able to outlast the retrieval it is waiting for. Otherwise, a thread that
        // loses the cache-lock race can time out with "Timeout while waiting for cache refresh" while the
        // retrieval is still progressing normally. Nimbus does not enforce this relationship, so we validate
        // it here instead of relying on a property-file comment.
        // Worst case: connect timeout + read timeout.
        long worstCaseRetrievalMs = (long) connectTimeoutMs + readTimeoutMs;
        require(cacheRefreshTimeoutMs > worstCaseRetrievalMs,
            "oidc.jwks.cache-refresh-timeout-ms (%d) must be greater than oidc.jwks.connect-timeout-ms plus "
                + "oidc.jwks.read-timeout-ms (%d + %d = %d); otherwise a thread waiting on an in-flight retrieval "
                + "gives up before that retrieval can complete, which is the failure this configuration exists to "
                + "prevent",
            cacheRefreshTimeoutMs, connectTimeoutMs, readTimeoutMs, worstCaseRetrievalMs);

        // Reproduces JWKSourceBuilder's own constraints, so the message names the property rather than surfacing as
        // an IllegalStateException from deep inside the builder.
        require(rateLimitMinIntervalMs < cacheTtlMs,
            "oidc.jwks.rate-limit-min-interval-ms (%d) must be less than oidc.jwks.cache-ttl-ms (%d)",
            rateLimitMinIntervalMs, cacheTtlMs);
        require(refreshAheadTimeMs + cacheRefreshTimeoutMs <= cacheTtlMs,
            "oidc.jwks.refresh-ahead-time-ms plus oidc.jwks.cache-refresh-timeout-ms (%d + %d = %d) must not "
                + "exceed oidc.jwks.cache-ttl-ms (%d)",
            refreshAheadTimeMs, cacheRefreshTimeoutMs, refreshAheadTimeMs + cacheRefreshTimeoutMs, cacheTtlMs);

        // Outage tolerance shorter than the cache TTL would never be reached: the cached set is still being served
        // from the caching layer for the whole of its own TTL.
        require(outageToleranceMs > cacheTtlMs,
            "oidc.jwks.outage-tolerance-ms (%d) must be greater than oidc.jwks.cache-ttl-ms (%d), otherwise the "
                + "outage cache expires no later than the ordinary cache and tolerates nothing",
            outageToleranceMs, cacheTtlMs);
    }

    private static void require(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}
