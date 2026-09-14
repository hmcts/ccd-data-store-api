package uk.gov.hmcts.ccd.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwksPropertiesTest {

    private static final String URI = "http://localhost:5000/o/jwks";

    @Test
    @DisplayName("accepts the production defaults")
    void acceptsProductionDefaults() {
        assertThatCode(() -> productionDefaults().build()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("parses the configured URI into a URL")
    void parsesUri() {
        assertThat(productionDefaults().build().url()).hasToString(URI);
    }

    @Test
    @DisplayName("rejects a URI that is not a usable URL")
    void rejectsMalformedUri() {
        JwksProperties properties = productionDefaults().uri("not a uri").build();

        assertThatThrownBy(properties::url)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid oidc.jwks.uri");
    }

    @Nested
    @DisplayName("relationships between values")
    class Relationships {

        @Test
        @DisplayName("rejects a cache refresh timeout that a retrieval could outlive")
        void rejectsCacheRefreshTimeoutBelowRetrievalWorstCase() {
            // 2000 + 5000 = 7000ms worst case for retrieval; a waiter given 6000ms always gives up first. This is
            // the production failure mode, and the check exists, so it cannot be reintroduced by configuration.
            assertThatThrownBy(() -> productionDefaults()
                .connectTimeoutMs(2000)
                .readTimeoutMs(5000)
                .cacheRefreshTimeoutMs(6000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms (6000)")
                .hasMessageContaining("2000 + 5000 = 7000");
        }

        @Test
        @DisplayName("rejects a cache refresh timeout exactly equal to the retrieval worst case")
        void rejectsCacheRefreshTimeoutEqualToRetrievalWorstCase() {
            assertThatThrownBy(() -> productionDefaults()
                .connectTimeoutMs(2000)
                .readTimeoutMs(5000)
                .cacheRefreshTimeoutMs(7000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms");
        }

        @Test
        @DisplayName("rejects a rate limit interval that is not shorter than the cache TTL")
        void rejectsRateLimitAboveCacheTtl() {
            assertThatThrownBy(() -> productionDefaults()
                .cacheTtlMs(30_000)
                .rateLimitMinIntervalMs(30_000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate-limit-min-interval-ms");
        }

        @Test
        @DisplayName("rejects a refresh-ahead window that does not fit inside the cache TTL")
        void rejectsRefreshAheadExceedingCacheTtl() {
            assertThatThrownBy(() -> productionDefaults()
                .cacheTtlMs(60_000)
                .refreshAheadTimeMs(55_000)
                .cacheRefreshTimeoutMs(10_000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-ahead-time-ms plus oidc.jwks.cache-refresh-timeout-ms");
        }

        @Test
        @DisplayName("rejects an outage tolerance that expires no later than the cache itself")
        void rejectsOutageToleranceBelowCacheTtl() {
            assertThatThrownBy(() -> productionDefaults()
                .cacheTtlMs(300_000)
                .outageToleranceMs(300_000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outage-tolerance-ms");
        }
    }

    @Nested
    @DisplayName("individual values")
    class IndividualValues {

        @Test
        @DisplayName("rejects an infinite connect timeout")
        void rejectsZeroConnectTimeout() {
            assertThatThrownBy(() -> productionDefaults().connectTimeoutMs(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout-ms")
                .hasMessageContaining("infinite");
        }

        @Test
        @DisplayName("rejects an infinite read timeout")
        void rejectsZeroReadTimeout() {
            assertThatThrownBy(() -> productionDefaults().readTimeoutMs(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout-ms")
                .hasMessageContaining("infinite");
        }

        @Test
        @DisplayName("rejects a blank URI")
        void rejectsBlankUri() {
            assertThatThrownBy(() -> productionDefaults().uri("  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidc.jwks.uri must be set");
        }
    }

    @Nested
    @DisplayName("binding")
    class Binding {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableJwksProperties.class);

        @Test
        @DisplayName("binds the kebab-case property names")
        void bindsProperties() {
            contextRunner
                .withPropertyValues(
                    "oidc.jwks.uri=" + URI,
                    "oidc.jwks.connect-timeout-ms=1500",
                    "oidc.jwks.read-timeout-ms=4000",
                    "oidc.jwks.size-limit-bytes=12345",
                    "oidc.jwks.cache-ttl-ms=200000",
                    "oidc.jwks.cache-refresh-timeout-ms=9000",
                    "oidc.jwks.refresh-ahead-time-ms=45000",
                    "oidc.jwks.rate-limit-min-interval-ms=20000",
                    "oidc.jwks.outage-tolerance-ms=3600000")
                .run(context -> assertThat(context).hasNotFailed()
                    .getBean(JwksProperties.class)
                    .isEqualTo(new JwksProperties(URI, 1500, 4000, 12345, 200_000L, 9_000L, 45_000L, 20_000L,
                        3_600_000L)));
        }

        @Test
        @DisplayName("fails the context when the values do not hold together, rather than at the first outage")
        void failsFastOnInvalidCombination() {
            contextRunner
                .withPropertyValues(
                    "oidc.jwks.uri=" + URI,
                    "oidc.jwks.connect-timeout-ms=2000",
                    "oidc.jwks.read-timeout-ms=5000",
                    "oidc.jwks.cache-refresh-timeout-ms=3000")
                .run(context -> assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cache-refresh-timeout-ms"));
        }
    }

    @EnableConfigurationProperties(JwksProperties.class)
    static class EnableJwksProperties {
    }

    private static Builder productionDefaults() {
        return new Builder();
    }

    /**
     * Mutable holder that mirrors the record, so each test can vary one value against a valid baseline without
     * repeating the other eight. The baseline is the set of defaults in {@code application.properties}.
     */
    private static final class Builder {
        private String uri = URI;
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int sizeLimitBytes = 51200;
        private long cacheTtlMs = 300_000L;
        private long cacheRefreshTimeoutMs = 10_000L;
        private long refreshAheadTimeMs = 60_000L;
        private long rateLimitMinIntervalMs = 30_000L;
        private long outageToleranceMs = 21_600_000L;

        Builder uri(String value) {
            this.uri = value;
            return this;
        }

        Builder connectTimeoutMs(int value) {
            this.connectTimeoutMs = value;
            return this;
        }

        Builder readTimeoutMs(int value) {
            this.readTimeoutMs = value;
            return this;
        }

        Builder cacheTtlMs(long value) {
            this.cacheTtlMs = value;
            return this;
        }

        Builder cacheRefreshTimeoutMs(long value) {
            this.cacheRefreshTimeoutMs = value;
            return this;
        }

        Builder refreshAheadTimeMs(long value) {
            this.refreshAheadTimeMs = value;
            return this;
        }

        Builder rateLimitMinIntervalMs(long value) {
            this.rateLimitMinIntervalMs = value;
            return this;
        }

        Builder outageToleranceMs(long value) {
            this.outageToleranceMs = value;
            return this;
        }

        JwksProperties build() {
            return new JwksProperties(uri, connectTimeoutMs, readTimeoutMs, sizeLimitBytes, cacheTtlMs,
                cacheRefreshTimeoutMs, refreshAheadTimeMs, rateLimitMinIntervalMs, outageToleranceMs);
        }
    }
}
