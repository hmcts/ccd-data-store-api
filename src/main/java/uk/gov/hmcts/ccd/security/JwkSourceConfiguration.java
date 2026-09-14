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
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import java.util.HashSet;
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
 *     <li>retrying once after a network failure, and only then falling back to the last known keys for
 *         {@link JwksProperties#outageToleranceMs()}, with the remaining outage window reported through
 *         {@link JwkSourceTelemetry}.</li>
 * </ul>
 *
 * <p>The outage tolerance is a deliberate trade-off. If IDAM revokes a signing key while it is unavailable,
 * tokens signed with that key can still be accepted until the tolerance window expires. The window is therefore
 * bounded and starts again from the most recent successful retrieval. A successful retrieval during an outage
 * restores the full tolerance window.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JwksProperties.class)
public class JwkSourceConfiguration {

    /**
     * Used only when the key set cannot be retrieved at start-up. RS256 is Spring Security's own default and what
     * IDAM signs with; deriving the algorithms from the live key set is preferred where possible.
     *
     * <p>Note that this set is fixed for the lifetime of the JVM once start-up has taken this branch: the
     * {@link JWSVerificationKeySelector} is built once. See CCD-8077 for why that is accepted rather than fixed.
     */
    static final Set<JWSAlgorithm> FALLBACK_ALGORITHMS = Set.of(JWSAlgorithm.RS256);

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
     * the layers are retry, outage tolerance, health reporting, rate limiting, and refresh-ahead caching.
     *
     * <p>Rate limiting is outside the outage-tolerant layer, so a {@code RateLimitReachedException}
     * is not handled by the outage cache and will be propagated to the caller.
     */
    @Bean
    public JWKSource<SecurityContext> idamJwkSource(JwkSourceTelemetry telemetry) {
        log.info("Configuring IDAM JWK source for {} (connect {}ms, read {}ms, cache TTL {}ms, "
                + "refresh ahead {}ms, rate limit {}ms, outage tolerance {}ms)",
            properties.uri(), properties.connectTimeoutMs(), properties.readTimeoutMs(), properties.cacheTtlMs(),
            properties.refreshAheadTimeMs(), properties.rateLimitMinIntervalMs(), properties.outageToleranceMs());

        return JWKSourceBuilder.create(properties.url(),
                new ObservedResourceRetriever(properties.connectTimeoutMs(), properties.readTimeoutMs(),
                    properties.sizeLimitBytes(), telemetry))
            .retrying(telemetry.retryingEventListener())
            .outageTolerant(properties.outageToleranceMs(), telemetry.outageEventListener())
            .healthReporting(telemetry.healthReportListener())
            .rateLimited(properties.rateLimitMinIntervalMs(), telemetry.rateLimitedEventListener())
            .cache(properties.cacheTtlMs(), properties.cacheRefreshTimeoutMs())
            .refreshAheadCache(properties.refreshAheadTimeMs(), true, telemetry.cachingEventListener())
            .build();
    }

    @Bean
    public JWSKeySelector<SecurityContext> idamJwsKeySelector(JWKSource<SecurityContext> idamJwkSource) {
        return new JWSVerificationKeySelector<>(signatureAlgorithms(idamJwkSource), idamJwkSource);
    }

    /**
     * Uses the same signature algorithms as {@code JwtDecoderProviderConfigurationUtils#getJWSAlgorithms},
     * which was previously used by {@code JwtDecoders.fromOidcIssuerLocation}. This keeps the accepted
     * algorithms unchanged while allowing the application to start even when IDAM is unavailable.
     */
    private Set<JWSAlgorithm> signatureAlgorithms(JWKSource<SecurityContext> jwkSource) {
        JWKMatcher matcher = new JWKMatcher.Builder()
            .publicOnly(true)
            .keyUses(KeyUse.SIGNATURE, null)
            .keyTypes(KeyType.RSA, KeyType.EC)
            .build();

        try {
            Set<JWSAlgorithm> algorithms = new HashSet<>();
            List<JWK> jwks = jwkSource.get(new JWKSelector(matcher), null);

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
                return algorithms;
            }

            log.warn("IDAM JWK set at {} advertised no usable signature algorithms; falling back to {}",
                properties.uri(), FALLBACK_ALGORITHMS);
        } catch (KeySourceException e) {
            log.warn("Unable to retrieve IDAM JWK set from {} at start-up; falling back to {}. "
                + "Token verification will retry against IDAM on the first request.", properties.uri(),
                FALLBACK_ALGORITHMS, e);
        }

        return FALLBACK_ALGORITHMS;
    }
}
