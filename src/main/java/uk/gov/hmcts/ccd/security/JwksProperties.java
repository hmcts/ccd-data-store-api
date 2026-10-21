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
 * <p>These settings are related, and some combinations are not valid. Nimbus
 * {@link com.nimbusds.jose.jwk.source.JWKSourceBuilder} rejects some combinations itself,
 * while another combination can lead to the production failure this configuration is intended
 * to prevent.
 *
 * <p>They are therefore checked in validate()
 * when the bean is created. This ensures configuration errors are caught at start-up rather than when the first token
 * needs to be verified during an IDAM outage.
 *
 * @param uri                    JWK set endpoint
 * @param connectTimeoutMs       TCP connect timeout for retrieval
 * @param readTimeoutMs          socket read timeout for retrieval
 * @param retrievalDeadlineMs    how long the calling thread waits for one request before abandoning it
 *                               ({@link TimeLimitedResourceRetriever}). Each retry is a new request with its own
 *                               deadline
 * @param sizeLimitBytes         maximum accepted response size
 * @param cacheTtlMs             how long a retrieved key set can be used before it needs refreshing
 * @param cacheRefreshTimeoutMs  how long a thread waits for another thread's in-flight retrieval
 * @param refreshAheadTimeMs     how far before expiry the background refresh is scheduled
 * @param rateLimitMinIntervalMs minimum interval between retrievals reaching IDAM
 * @param outageToleranceMs      how long the last retrieved key set can be used while IDAM is unavailable
 * @param retrying               whether a failed retrieval is retried before the outage cache is used
 */
@ConfigurationProperties("oidc.jwks")
public record JwksProperties(
    String uri,
    @DefaultValue("2000") int connectTimeoutMs,
    @DefaultValue("5000") int readTimeoutMs,
    @DefaultValue("2500") int retrievalDeadlineMs,
    @DefaultValue("51200") int sizeLimitBytes,
    @DefaultValue("300000") long cacheTtlMs,
    @DefaultValue("17000") long cacheRefreshTimeoutMs,
    @DefaultValue("60000") long refreshAheadTimeMs,
    @DefaultValue("30000") long rateLimitMinIntervalMs,
    @DefaultValue("21600000") long outageToleranceMs,
    @DefaultValue("true") boolean retrying
) {

    /**
     * {@code RetryingJWKSetSource} retries exactly once. The count is fixed in Nimbus and cannot be configured;
     * {@code JwkSourceConfigurationTest} checks it against the library so an upgrade that changes it fails the build.
     */
    static final int NIMBUS_RETRIES = 1;

    public JwksProperties {
        validate(uri, connectTimeoutMs, readTimeoutMs, retrievalDeadlineMs, sizeLimitBytes, cacheTtlMs,
            cacheRefreshTimeoutMs, refreshAheadTimeMs, rateLimitMinIntervalMs, outageToleranceMs, retrying);
    }

    /**
     * The number of HTTP requests a single retrieval can make before it fails.
     */
    public int retrievalAttempts() {
        return retrievalAttempts(retrying);
    }

    private static int retrievalAttempts(boolean retrying) {
        return retrying ? 1 + NIMBUS_RETRIES : 1;
    }

    /**
     * The longest the calling thread waits for one request: the deadline, wherever the request is stuck (DNS,
     * connect, headers, body). See {@link TimeLimitedResourceRetriever}.
     */
    public long attemptWorstCaseMs() {
        return retrievalDeadlineMs;
    }

    /**
     * How often {@link JwkSetStartupRetry} retries while no key set has been retrieved since start-up: half the rate
     * limit interval, 15 s at the defaults.
     */
    public long startupRetryIntervalMs() {
        return Math.max(1L, rateLimitMinIntervalMs / 2);
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
                                 int retrievalDeadlineMs,
                                 int sizeLimitBytes,
                                 long cacheTtlMs,
                                 long cacheRefreshTimeoutMs,
                                 long refreshAheadTimeMs,
                                 long rateLimitMinIntervalMs,
                                 long outageToleranceMs,
                                 boolean retrying) {

        require(uri != null && !uri.isBlank(), "oidc.jwks.uri must be set");
        require(connectTimeoutMs > 0, "oidc.jwks.connect-timeout-ms must be greater than 0, but was %d; "
            + "0 means an infinite timeout, which is what let a single unreachable IDAM stall request threads "
            + "indefinitely", connectTimeoutMs);
        require(readTimeoutMs > 0, "oidc.jwks.read-timeout-ms must be greater than 0, but was %d; "
            + "0 means an infinite timeout, which is what let a single unreachable IDAM stall request threads "
            + "indefinitely", readTimeoutMs);
        require(retrievalDeadlineMs > 0, "oidc.jwks.retrieval-deadline-ms must be greater than 0, but was %d",
            retrievalDeadlineMs);
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
        // Worst case: every attempt the retrieval can make holds the lock for up to the deadline (see
        // attemptWorstCaseMs). The retry happens inside the lock the waiter is queued on, so the waiter has to outlast
        // all attempts, not one.
        int attempts = retrievalAttempts(retrying);
        long worstCaseRetrievalMs = (long) attempts * retrievalDeadlineMs;
        require(cacheRefreshTimeoutMs > worstCaseRetrievalMs,
            "oidc.jwks.cache-refresh-timeout-ms (%d) must be greater than the retrieval attempts times "
                + "oidc.jwks.retrieval-deadline-ms (%d x %d = %d); otherwise a thread waiting on an in-flight "
                + "retrieval gives up before that retrieval can complete, which is the failure this configuration "
                + "exists to prevent",
            cacheRefreshTimeoutMs, attempts, retrievalDeadlineMs, worstCaseRetrievalMs);

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
