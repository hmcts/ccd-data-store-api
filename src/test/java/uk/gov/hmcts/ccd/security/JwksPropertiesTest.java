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
        @DisplayName("rejects a cache refresh timeout sized for one attempt when the retrieval is retried")
        void rejectsCacheRefreshTimeoutSizedForOneAttempt() {
            // 4000ms outlasts one attempt (the 2500ms deadline) but not the retry, which runs inside the same lock:
            // 2 x 2500 = 5000ms. JwkSourceRefreshWaitIT shows a waiter sized like this timing out on a retrieval that
            // then succeeds.
            Builder builder = productionDefaults()
                .retrievalDeadlineMs(2500)
                .cacheRefreshTimeoutMs(4000);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms (4000)")
                .hasMessageContaining("2 x 2500 = 5000");
        }

        @Test
        @DisplayName("names the formula in the message")
        void namesTheFormula() {
            assertThatThrownBy(productionDefaults().cacheRefreshTimeoutMs(4000)::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oidc.jwks.cache-refresh-timeout-ms (4000) must be greater than the retrieval attempts "
                    + "times oidc.jwks.retrieval-deadline-ms (2 x 2500 = 5000); otherwise a thread waiting on an "
                    + "in-flight retrieval gives up before that retrieval can complete, which is the failure this "
                    + "configuration exists to prevent");
        }

        @Test
        @DisplayName("rejects a cache refresh timeout exactly equal to the retrieval worst case")
        void rejectsCacheRefreshTimeoutEqualToRetrievalWorstCase() {
            Builder builder = productionDefaults().retrievalDeadlineMs(2500);

            assertThatThrownBy(builder.cacheRefreshTimeoutMs(5000)::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms (5000)");
            assertThatCode(builder.cacheRefreshTimeoutMs(5001)::build).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts the production defaults: 17000 > 2 x 2500")
        void productionDefaultsPassTheFormula() {
            JwksProperties properties = productionDefaults().build();

            assertThat(properties.retrievalDeadlineMs()).isEqualTo(2500);
            assertThat(properties.attemptWorstCaseMs()).isEqualTo(2500L);
            assertThat(properties.cacheRefreshTimeoutMs())
                .isEqualTo(17_000L)
                .isGreaterThan(properties.retrievalAttempts() * properties.attemptWorstCaseMs());
        }

        @Test
        @DisplayName("does not count the connect and read timeouts: the caller waits for the deadline at most")
        void doesNotCountTheConnectAndReadTimeouts() {
            JwksProperties properties = productionDefaults()
                .connectTimeoutMs(10_000)
                .readTimeoutMs(30_000)
                .build();

            assertThat(properties.attemptWorstCaseMs()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("rejects a deadline too long for the production wait: 2 x 8500 = 17000")
        void rejectsALongerDeadlineAgainstTheDefaultWait() {
            assertThatCode(productionDefaults().retrievalDeadlineMs(8499)::build).doesNotThrowAnyException();
            assertThatThrownBy(productionDefaults().retrievalDeadlineMs(8500)::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms (17000)")
                .hasMessageContaining("(2 x 8500 = 17000)");
        }

        @Test
        @DisplayName("sizes the worst case for a single attempt when retrying is disabled")
        void sizesWorstCaseForOneAttemptWithoutRetry() {
            assertThatCode(() -> productionDefaults().retrying(false).cacheRefreshTimeoutMs(2600).build())
                .doesNotThrowAnyException();

            assertThatThrownBy(productionDefaults().retrying(false).cacheRefreshTimeoutMs(2500)::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 x 2500 = 2500");
        }

        @Test
        @DisplayName("counts the retry as a second attempt")
        void countsRetrievalAttempts() {
            assertThat(productionDefaults().build().retrievalAttempts()).isEqualTo(2);
            assertThat(productionDefaults().retrying(false).build().retrievalAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects a rate limit interval that is not shorter than the cache TTL")
        void rejectsRateLimitAboveCacheTtl() {
            Builder builder = productionDefaults()
                .cacheTtlMs(30_000)
                .rateLimitMinIntervalMs(30_000);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate-limit-min-interval-ms");
        }

        @Test
        @DisplayName("rejects a refresh-ahead window that does not fit inside the cache TTL")
        void rejectsRefreshAheadExceedingCacheTtl() {
            Builder builder = productionDefaults()
                .cacheTtlMs(60_000)
                .refreshAheadTimeMs(55_000)
                .cacheRefreshTimeoutMs(17_000);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-ahead-time-ms plus oidc.jwks.cache-refresh-timeout-ms");
        }

        @Test
        @DisplayName("rejects an outage tolerance that expires no later than the cache itself")
        void rejectsOutageToleranceBelowCacheTtl() {
            Builder builder = productionDefaults()
                .cacheTtlMs(300_000)
                .outageToleranceMs(300_000);

            assertThatThrownBy(builder::build)
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
            Builder builder = productionDefaults().connectTimeoutMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout-ms")
                .hasMessageContaining("infinite");
        }

        @Test
        @DisplayName("rejects an infinite read timeout")
        void rejectsZeroReadTimeout() {
            Builder builder = productionDefaults().readTimeoutMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout-ms")
                .hasMessageContaining("infinite");
        }

        @Test
        @DisplayName("rejects a zero deadline")
        void rejectsZeroDeadline() {
            Builder builder = productionDefaults().retrievalDeadlineMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidc.jwks.retrieval-deadline-ms must be greater than 0, but was 0");
        }

        @Test
        @DisplayName("rejects a blank URI")
        void rejectsBlankUri() {
            Builder builder = productionDefaults().uri("  ");

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidc.jwks.uri must be set");
        }

        @Test
        @DisplayName("rejects a zero size limit")
        void rejectsZeroSizeLimit() {
            Builder builder = productionDefaults().sizeLimitBytes(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size-limit-bytes");
        }

        @Test
        @DisplayName("rejects a zero cache TTL")
        void rejectsZeroCacheTtl() {
            Builder builder = productionDefaults().cacheTtlMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-ttl-ms");
        }

        @Test
        @DisplayName("rejects a zero cache refresh timeout")
        void rejectsZeroCacheRefreshTimeout() {
            Builder builder = productionDefaults().cacheRefreshTimeoutMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-refresh-timeout-ms must be greater than 0");
        }

        @Test
        @DisplayName("rejects a zero refresh-ahead time")
        void rejectsZeroRefreshAheadTime() {
            Builder builder = productionDefaults().refreshAheadTimeMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-ahead-time-ms must be greater than 0");
        }

        @Test
        @DisplayName("rejects a zero rate limit interval")
        void rejectsZeroRateLimitInterval() {
            Builder builder = productionDefaults().rateLimitMinIntervalMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate-limit-min-interval-ms must be greater than 0");
        }

        @Test
        @DisplayName("rejects a zero outage tolerance")
        void rejectsZeroOutageTolerance() {
            Builder builder = productionDefaults().outageToleranceMs(0);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outage-tolerance-ms must be greater than 0");
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
                    "oidc.jwks.retrieval-deadline-ms=1800",
                    "oidc.jwks.size-limit-bytes=12345",
                    "oidc.jwks.cache-ttl-ms=200000",
                    "oidc.jwks.cache-refresh-timeout-ms=12000",
                    "oidc.jwks.refresh-ahead-time-ms=45000",
                    "oidc.jwks.rate-limit-min-interval-ms=20000",
                    "oidc.jwks.outage-tolerance-ms=3600000")
                .run(context -> assertThat(context).hasNotFailed()
                    .getBean(JwksProperties.class)
                    .isEqualTo(new JwksProperties(URI, 1500, 4000, 1800, 12345, 200_000L, 12_000L, 45_000L,
                        20_000L, 3_600_000L, true)));
        }

        @Test
        @DisplayName("defaults the deadline to 2500ms")
        void defaultsTheDeadline() {
            contextRunner
                .withPropertyValues("oidc.jwks.uri=" + URI)
                .run(context -> assertThat(context).hasNotFailed()
                    .getBean(JwksProperties.class)
                    .isEqualTo(productionDefaults().build()));
        }

        @Test
        @DisplayName("binds the retrying flag, which changes the worst case the refresh timeout must exceed")
        void bindsRetrying() {
            contextRunner
                .withPropertyValues(
                    "oidc.jwks.uri=" + URI,
                    "oidc.jwks.retrying=false",
                    "oidc.jwks.cache-refresh-timeout-ms=10000")
                .run(context -> assertThat(context).hasNotFailed()
                    .getBean(JwksProperties.class)
                    .satisfies(properties -> {
                        assertThat(properties.retrying()).isFalse();
                        assertThat(properties.retrievalAttempts()).isEqualTo(1);
                    }));
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
     * repeating the other ten. The baseline is the set of defaults in {@code application.properties}.
     */
    private static final class Builder {
        private String uri = URI;
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int retrievalDeadlineMs = 2500;
        private int sizeLimitBytes = 51200;
        private long cacheTtlMs = 300_000L;
        private long cacheRefreshTimeoutMs = 17_000L;
        private long refreshAheadTimeMs = 60_000L;
        private long rateLimitMinIntervalMs = 30_000L;
        private long outageToleranceMs = 21_600_000L;
        private boolean retrying = true;

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

        Builder retrievalDeadlineMs(int value) {
            this.retrievalDeadlineMs = value;
            return this;
        }

        Builder sizeLimitBytes(int value) {
            this.sizeLimitBytes = value;
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

        Builder retrying(boolean value) {
            this.retrying = value;
            return this;
        }

        JwksProperties build() {
            return new JwksProperties(uri, connectTimeoutMs, readTimeoutMs, retrievalDeadlineMs, sizeLimitBytes,
                cacheTtlMs, cacheRefreshTimeoutMs, refreshAheadTimeMs, rateLimitMinIntervalMs, outageToleranceMs,
                retrying);
        }
    }
}
