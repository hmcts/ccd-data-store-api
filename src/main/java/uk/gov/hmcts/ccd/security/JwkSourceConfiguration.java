package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.jwk.source.URLBasedJWKSetSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@link JWKSource} used to verify IDAM-issued user tokens.
 *
 * <p>Spring Security's default source (see {@code NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder#jwkSource})
 * caches the JWK set but does not use refresh-ahead or rate limiting. It also uses a plain {@code RestTemplate}
 * without HTTP timeouts. When IDAM is unavailable and the cache needs refreshing, the refresh thread can block
 * on the OS-level TCP timeout while holding the cache lock. Concurrent requests then fail with
 * "Timeout while waiting for cache refresh" once {@code cacheRefreshTimeout} expires.
 *
 * <p>This source avoids that behaviour by:
 * <ul>
 *     <li>using explicit connect and read timeouts, so network failures are detected within seconds;</li>
 *     <li>refreshing keys in the background before they expire, so user requests do not wait for a refresh;</li>
 *     <li>retrying once after failed retrieval ({@link JwksProperties#retrying()}), and only then falling back
 *         to the last known keys for {@link JwksProperties#outageToleranceMs()}, with the remaining outage window
 *         reported through {@link JwkSourceTelemetry}.</li>
 * </ul>
 *
 * <p>The outage tolerance is a deliberate trade-off. If IDAM revokes a signing key while it is unavailable,
 * tokens signed with that key can still be accepted until the tolerance window closes, and for up to
 * {@link JwksProperties#cacheTtlMs()} after that, because the outer cache re-caches the last stale set it was given
 * (just under 6 h 05 m at the defaults). The window is therefore bounded and starts again from the most recent
 * successful retrieval. Successful retrieval during an outage restores the full tolerance window.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JwksProperties.class)
public class JwkSourceConfiguration {

    /**
     * Used only when start-up cannot derive the algorithms from the live key set: IDAM is unreachable, or its key
     * set has no usable signing key.
     *
     * <p>It is the RSA family (RS256, RS384, RS512, PS256, PS384, PS512) because that is what a healthy start-up
     * derives today. IDAM's signing key is an RSA key that publishes no {@code alg}, and
     * {@code signatureAlgorithms} accepts the whole RSA family for such a key. A pod that started during an outage
     * therefore accepts the same algorithms as one that started normally.
     *
     * <p>Like the derived set, this set is fixed for the lifetime of the JVM: the {@link JWSVerificationKeySelector}
     * is built once, and IDAM recovering later does not change it. It differs from what a healthy start-up would
     * derive only if IDAM's key set changes:
     * <ul>
     *     <li>IDAM adds a signing key that is not RSA, for example, EC. Tokens signed with it get 401 on a pod that
     *         fell back, until that pod restarts. This fails closed. If IDAM publishes only EC keys, the pod becomes
     *         ready, since an EC key is a usable signing key, but rejects every token until it restarts, like any pod
     *         that derived RSA at start-up.</li>
     *     <li>IDAM starts publishing an {@code alg} on its RSA key. A healthy start-up then derives only that
     *         algorithm, so a pod that fell back accepts more than a healthy one. It still accepts only RSA
     *         algorithms, and a token still needs a valid signature from one of IDAM's RSA keys.</li>
     * </ul>
     *
     * <p>{@link JwkSourceTelemetry} reports a fallback: an {@code ALGORITHMS_FALLBACK} event at start-up, with the
     * fallback set in its {@code algorithms} dimension, and {@code algorithmSource=FALLBACK} on every later event.
     */
    static final Set<JWSAlgorithm> FALLBACK_ALGORITHMS =
        Collections.unmodifiableSet(new LinkedHashSet<>(JWSAlgorithm.Family.RSA));

    /**
     * The keys that can verify an IDAM token: public RSA or EC keys whose {@code use} is {@code sig} or absent. It is
     * the matcher of {@code JwtDecoderProviderConfigurationUtils#getJWSAlgorithms}, which failed start-up on master
     * when nothing matched.
     *
     * <p>{@link #signatureAlgorithms} derives the algorithms from these keys, and readiness waits for a key set that
     * has at least one of them ({@link ObservedJWKSetSource}, {@link JwkSetStartupRetry}). A key set that parses but
     * has none, for example, an empty one or one with only {@code enc} keys, verifies no token, so it must not make
     * the instance ready.
     */
    static final JWKMatcher USABLE_SIGNING_KEYS = new JWKMatcher.Builder()
        .publicOnly(true)
        .keyUses(KeyUse.SIGNATURE, null)
        .keyTypes(KeyType.RSA, KeyType.EC)
        .build();

    private final JwksProperties properties;

    public JwkSourceConfiguration(JwksProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwkSourceTelemetry jwkSourceTelemetry(AppInsights appInsights) {
        return new JwkSourceTelemetry(appInsights);
    }

    /**
     * The bean is {@link java.io.Closeable}, so Spring automatically shuts down the refresh-ahead
     * executors when the application context closes.
     *
     * <p>The layer order is fixed by {@code JWKSourceBuilder#build()}. From the network outwards,
     * the layers are {@link ObservedJWKSetSource}, retry, outage tolerance, health reporting, rate limiting, and
     * refresh-ahead caching.
     *
     * <p>Rate limiting is outside the outage-tolerant layer, so a {@code RateLimitReachedException}
     * is not handled by the outage cache and will be propagated to the caller.
     */
    @Bean
    public JWKSource<SecurityContext> idamJwkSource(JwkSourceTelemetry telemetry) {
        log.info("Configuring IDAM JWK source for {} (connect {}ms, read {}ms, attempts {}, cache TTL {}ms, "
                + "cache refresh timeout {}ms, refresh ahead {}ms, rate limit {}ms, outage tolerance {}ms)",
            properties.uri(), properties.connectTimeoutMs(), properties.readTimeoutMs(),
            properties.retrievalAttempts(), properties.cacheTtlMs(), properties.cacheRefreshTimeoutMs(),
            properties.refreshAheadTimeMs(), properties.rateLimitMinIntervalMs(), properties.outageToleranceMs());

        URLBasedJWKSetSource<SecurityContext> urlSource = new URLBasedJWKSetSource<>(properties.url(),
            new DefaultResourceRetriever(properties.connectTimeoutMs(), properties.readTimeoutMs(),
                properties.sizeLimitBytes()));

        JWKSourceBuilder<SecurityContext> builder =
            JWKSourceBuilder.create(new ObservedJWKSetSource<>(urlSource, telemetry));
        if (properties.retrying()) {
            builder.retrying(telemetry.retryingEventListener());
        }

        return builder
            .outageTolerant(properties.outageToleranceMs(), telemetry.outageEventListener())
            .healthReporting(telemetry.healthReportListener())
            .rateLimited(properties.rateLimitMinIntervalMs(), telemetry.rateLimitedEventListener())
            .cache(properties.cacheTtlMs(), properties.cacheRefreshTimeoutMs())
            .refreshAheadCache(properties.refreshAheadTimeMs(), true, telemetry.cachingEventListener())
            .build();
    }

    @Bean
    public JWSKeySelector<SecurityContext> idamJwsKeySelector(JWKSource<SecurityContext> idamJwkSource,
                                                              JwkSourceTelemetry telemetry) {
        return new JWSVerificationKeySelector<>(signatureAlgorithms(idamJwkSource, telemetry), idamJwkSource);
    }

    /**
     * Not ready until a usable signing key has been retrieved once. See {@link IdamJwksHealthIndicator}; the bean
     * name gives the health contributor its name, {@code idamJwks}, which the readiness group includes.
     */
    @Bean
    public IdamJwksHealthIndicator idamJwksHealthIndicator(JwkSourceTelemetry telemetry) {
        return new IdamJwksHealthIndicator(telemetry);
    }

    @Bean
    public JwkSetStartupRetry jwkSetStartupRetry(JWKSource<SecurityContext> idamJwkSource,
                                                 JwkSourceTelemetry telemetry) {
        return new JwkSetStartupRetry(idamJwkSource, telemetry, properties.startupRetryIntervalMs());
    }

    /**
     * Uses the same signature algorithms as {@code JwtDecoderProviderConfigurationUtils#getJWSAlgorithms},
     * which was previously used by {@code JwtDecoders.fromOidcIssuerLocation}. This keeps the accepted
     * algorithms unchanged while allowing the application to start even when IDAM is unavailable.
     */
    private Set<JWSAlgorithm> signatureAlgorithms(JWKSource<SecurityContext> jwkSource,
                                                  JwkSourceTelemetry telemetry) {
        try {
            Set<JWSAlgorithm> algorithms = new HashSet<>();
            List<JWK> jwks = jwkSource.get(new JWKSelector(USABLE_SIGNING_KEYS), null);

            for (JWK jwk : jwks) {
                if (jwk.getAlgorithm() != null) {
                    algorithms.add(JWSAlgorithm.parse(jwk.getAlgorithm().getName()));
                } else if (KeyType.RSA.equals(jwk.getKeyType())) {
                    algorithms.addAll(JWSAlgorithm.Family.RSA);
                } else if (KeyType.EC.equals(jwk.getKeyType())) {
                    algorithms.addAll(JWSAlgorithm.Family.EC);
                }
            }

            if (!algorithms.isEmpty()) {
                telemetry.algorithmsDerived(algorithms);
                return algorithms;
            }

            log.warn("IDAM JWK set at {} advertised no usable signature algorithms; falling back to the RSA family "
                + "{} for the lifetime of this instance. The instance is not ready (health check idamJwks) until a "
                + "usable signing key is retrieved, retried in the background every {}ms.",
                properties.uri(), FALLBACK_ALGORITHMS, properties.startupRetryIntervalMs());
            telemetry.algorithmsFellBack(FALLBACK_ALGORITHMS, null);
        } catch (KeySourceException e) {
            log.warn("Unable to retrieve IDAM JWK set from {} at start-up; falling back to the RSA family {} for "
                + "the lifetime of this instance. The instance is not ready (health check idamJwks) until a usable "
                + "signing key is retrieved, retried in the background every {}ms.",
                properties.uri(), FALLBACK_ALGORITHMS, properties.startupRetryIntervalMs(), e);
            telemetry.algorithmsFellBack(FALLBACK_ALGORITHMS, e);
        }

        return FALLBACK_ALGORITHMS;
    }
}
