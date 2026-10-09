package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.CapturedLogs;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.EMPTY_JWK_SET;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.ORIGINAL_KEY_ID;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.encryptionAndSigningJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.encryptionOnlyJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.jwksUri;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.retrievalAttempts;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubDown;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.stubHealthy;

/**
 * Drives {@link JwkSetStartupRetry} against a stubbed IDAM with no requests at all, as on an instance that is not
 * ready and so gets none.
 */
class JwkSetStartupRetryIT {

    private static final JWKSelector ANY_KEY = new JWKSelector(new JWKMatcher.Builder().build());
    private static final JWKSelector USABLE_SIGNING_KEY =
        new JWKSelector(JwkSourceConfiguration.USABLE_SIGNING_KEYS);
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL = Duration.ofMillis(25);

    // Half the rate limit interval, from JwksProperties#startupRetryIntervalMs().
    private static final long RATE_LIMIT_MS = 200L;
    private static final long RETRY_INTERVAL_MS = RATE_LIMIT_MS / 2;

    private static final String UNHEALTHY = "IDAM JWK set endpoint unhealthy";
    private static final String NO_USABLE_KEY = "IDAM JWK set contains no usable signing key; instance stays not ready";
    private static final String NO_USABLE_ALGORITHMS = "advertised no usable signature algorithms";
    private static final String RECOVERED_AFTER_FALLBACK =
        "IDAM JWK set retrieved; key source recovered after start-up fallback";

    private WireMockServer wireMock;
    private JWKSource<SecurityContext> jwkSource;
    private RecordingAppInsights appInsights;
    private JwkSourceTelemetry telemetry;
    private IdamJwksHealthIndicator indicator;
    private JwkSourceConfiguration configuration;
    private JwkSetStartupRetry retry;
    private CapturedLogs retryLogs;
    private CapturedLogs telemetryLogs;
    private CapturedLogs configurationLogs;
    private final List<Attempt> attempts = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
        appInsights = new RecordingAppInsights();
        telemetry = new JwkSourceTelemetry(appInsights);
        indicator = new IdamJwksHealthIndicator(telemetry);
        configuration = new JwkSourceConfiguration(properties());
        retryLogs = new CapturedLogs(JwkSetStartupRetry.class);
        telemetryLogs = new CapturedLogs(JwkSourceTelemetry.class);
        configurationLogs = new CapturedLogs(JwkSourceConfiguration.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (retry != null) {
            retry.stop();
        }
        retryLogs.close();
        telemetryLogs.close();
        configurationLogs.close();
        if (jwkSource instanceof Closeable closeable) {
            closeable.close();
        }
        wireMock.stop();
    }

    @Test
    @DisplayName("derives its interval from the rate limiter: 15s at the production defaults")
    void intervalIsHalfTheRateLimitInterval() {
        assertThat(properties().startupRetryIntervalMs()).isEqualTo(RETRY_INTERVAL_MS);
        assertThat(new JwksProperties("http://localhost/o/jwks", 2000, 5000, 51200, 300_000L, 16_000L, 60_000L,
            30_000L, 21_600_000L, true).startupRetryIntervalMs()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("with no requests, retries until IDAM returns, then fills the cache, arms the scheduled refresh and "
        + "stops")
    void retriesUntilIdamReturnsThenStops() throws Exception {
        stubDown(wireMock);
        startUp();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(retry.isRunning()).isTrue();

        int afterStartUp = retrievalAttempts(wireMock);
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(retrievalAttempts(wireMock))
                .as("background retrievals, each retried once, with no request made")
                .isGreaterThanOrEqualTo(afterStartUp + 3 * 2));
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);

        stubHealthy(wireMock, originalJwkSet());

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() -> {
            assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
            assertThat(retry.isRunning()).as("the retry thread has finished").isFalse();
        });

        // The cache was filled: the key comes back without another retrieval. The scheduled refresh is not due yet:
        // cache TTL 3000 - refresh ahead 1000 - refresh timeout 320 = 1680ms after the retrieval.
        int afterRecovery = retrievalAttempts(wireMock);
        assertThat(jwkSource.get(ANY_KEY, null)).extracting(JWK::getKeyID).containsExactly(ORIGINAL_KEY_ID);
        assertThat(retrievalAttempts(wireMock)).as("served from the cache").isEqualTo(afterRecovery);

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(appInsights.eventsOfType(JwkSourceTelemetry.SCHEDULED_REFRESH_COMPLETED))
                .as("the retry's retrieval armed the refresh-ahead schedule, which runs with no request")
                .isNotEmpty());

        assertThat(appInsights.eventsOfType(JwkSourceTelemetry.RATE_LIMIT_REACHED))
            .as("retries at half the rate limit interval stay within its two retrievals per interval")
            .isEmpty();
        assertThat(telemetryLogs.startingWith(RECOVERED_AFTER_FALLBACK))
            .singleElement()
            .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
    }

    @Test
    @DisplayName("logs nothing above DEBUG itself; each failed retry logs only the health layer's one WARN")
    void logsAtMostTheExistingWarnPerFailedRetry() {
        stubDown(wireMock);
        startUp();

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(retryLogs.startingWith("Background IDAM JWK set retrieval failed")).hasSizeGreaterThan(3));
        retry.stop();
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).until(() -> !retry.isRunning());

        assertThat(retryLogs.startingWith(""))
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));

        List<ILoggingEvent> telemetryAboveDebug = telemetryLogs.startingWith("").stream()
            .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
            .toList();
        long failedRetrievals = appInsights.eventsOfType(JwkSourceTelemetry.HEALTH).stream()
            .filter(event -> "NOT_HEALTHY".equals(event.properties().get(JwkSourceTelemetry.HEALTH_STATUS)))
            .count();
        assertThat(telemetryAboveDebug)
            .as("one WARN per failed retrieval, the start-up one included, and nothing else")
            .hasSize((int) failedRetrievals)
            .allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).startsWith(UNHEALTHY);
            });

        assertThat(configurationLogs.startingWith("").stream()
            .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN)))
            .as("the start-up fallback WARN, once, and it says the retry is running")
            .singleElement()
            .satisfies(event -> assertThat(event.getFormattedMessage())
                .contains("not ready (health check idamJwks)")
                .contains("retried in the background every " + RETRY_INTERVAL_MS + "ms"));
    }

    @Test
    @DisplayName("an empty key set: not ready and retrying, one WARN per attempt, then ready on a usable key")
    void emptyKeySetIsNotReadyUntilAUsableSigningKey() throws Exception {
        notReadyUntilAUsableSigningKey(EMPTY_JWK_SET);
    }

    @Test
    @DisplayName("a key set with only an encryption key: not ready and retrying, one WARN per attempt, then ready on a "
        + "usable key")
    void encryptionOnlyKeySetIsNotReadyUntilAUsableSigningKey() throws Exception {
        notReadyUntilAUsableSigningKey(encryptionOnlyJwkSet());
    }

    @Test
    @DisplayName("never runs when the start-up retrieval got a usable signing key among other keys")
    void neverRunsWhenStartUpGotAUsableSigningKeyAmongOthers() {
        stubHealthy(wireMock, encryptionAndSigningJwkSet());
        startUp();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(retry.isRunning()).isFalse();
        // Not "exactly one request": a cold WireMock can miss the first read timeout, and Nimbus then retries.
        int afterStartUp = retrievalAttempts(wireMock);
        await().during(Duration.ofMillis(5 * RETRY_INTERVAL_MS)).atMost(SETTLE_TIMEOUT).pollInterval(POLL)
            .until(() -> retrievalAttempts(wireMock) == afterStartUp);
        assertThat(attempts).isEmpty();
        assertThat(retryLogs.startingWith("")).isEmpty();
    }

    @Test
    @DisplayName("never runs when the start-up retrieval succeeded")
    void neverRunsWhenStartUpSucceeded() {
        stubHealthy(wireMock, originalJwkSet());
        startUp();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(retry.isRunning()).isFalse();
        // Not "exactly one request": a cold WireMock can miss the first read timeout, and Nimbus then retries.
        int afterStartUp = retrievalAttempts(wireMock);
        await().during(Duration.ofMillis(5 * RETRY_INTERVAL_MS)).atMost(SETTLE_TIMEOUT).pollInterval(POLL)
            .until(() -> retrievalAttempts(wireMock) == afterStartUp);
        assertThat(retryLogs.startingWith("")).isEmpty();
    }

    @Test
    @DisplayName("stops for good when the application context closes")
    void stopsOnContextClose() {
        stubDown(wireMock);
        startUp();
        int afterStartUp = retrievalAttempts(wireMock);
        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL)
            .until(() -> retrievalAttempts(wireMock) > afterStartUp);

        retry.stop();

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).until(() -> !retry.isRunning());
        int afterStop = retrievalAttempts(wireMock);
        await().during(Duration.ofMillis(5 * RETRY_INTERVAL_MS)).atMost(SETTLE_TIMEOUT).pollInterval(POLL)
            .until(() -> retrievalAttempts(wireMock) == afterStop);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    /**
     * IDAM answers, but with a key set that verifies no token. The instance must stay not ready, as master never
     * started, and retry with the readiness matcher, which makes Nimbus refresh the cached set rather than return it.
     */
    private void notReadyUntilAUsableSigningKey(String keySetWithoutUsableKey) throws Exception {
        stubHealthy(wireMock, keySetWithoutUsableKey);
        startUp();

        assertThat(telemetry.algorithmSource()).isEqualTo(JwkSourceTelemetry.AlgorithmSource.FALLBACK);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(retry.isRunning()).isTrue();

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() ->
            assertThat(retryLogs.startingWith(NO_USABLE_KEY))
                .as("background attempts that got the key set again")
                .hasSizeGreaterThanOrEqualTo(3));
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(retry.isRunning()).isTrue();
        assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.matcher())
            .as("the retry selects with the matcher that signatureAlgorithms() and readiness use")
            .isSameAs(JwkSourceConfiguration.USABLE_SIGNING_KEYS));

        stubHealthy(wireMock, originalJwkSet());

        await().atMost(SETTLE_TIMEOUT).pollInterval(POLL).untilAsserted(() -> {
            assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
            assertThat(retry.isRunning()).as("the retry thread has finished").isFalse();
        });

        int afterRecovery = retrievalAttempts(wireMock);
        assertThat(jwkSource.get(USABLE_SIGNING_KEY, null)).extracting(JWK::getKeyID).containsExactly(ORIGINAL_KEY_ID);
        assertThat(retrievalAttempts(wireMock)).as("served from the cache").isEqualTo(afterRecovery);

        assertOneWarningPerAttemptThatReachedIdam();
        assertThat(telemetryLogs.startingWith("").stream()
            .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO)))
            .as("IDAM answered every time, so nothing from the health or outage layers")
            .isEmpty();
        assertThat(retryLogs.startingWith("").stream()
            .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO)))
            .allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).startsWith(NO_USABLE_KEY);
            });
        assertThat(configurationLogs.startingWith("").stream()
            .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN)))
            .as("the start-up fallback WARN, once, and it says the retry is running")
            .singleElement()
            .satisfies(event -> assertThat(event.getFormattedMessage())
                .contains(NO_USABLE_ALGORITHMS)
                .contains("not ready (health check idamJwks)")
                .contains("retried in the background every " + RETRY_INTERVAL_MS + "ms"));
    }

    /**
     * Each attempt that reached IDAM, apart from the last, which got the usable key, logged exactly one WARN, and
     * each attempt that did not (refused by the rate limiter) logged none. An attempt's WARN follows its retrieval,
     * so it is counted by the time the next attempt starts.
     */
    private void assertOneWarningPerAttemptThatReachedIdam() {
        List<Attempt> made = List.copyOf(attempts);
        int warnings = retryLogs.startingWith(NO_USABLE_KEY).size();
        assertThat(made).hasSizeGreaterThanOrEqualTo(4);
        for (int i = 0; i < made.size(); i++) {
            Attempt attempt = made.get(i);
            boolean last = i == made.size() - 1;
            int warningsAfter = last ? warnings : made.get(i + 1).warningsAtStart();
            assertThat(warningsAfter - attempt.warningsAtStart())
                .as("WARNs from attempt %d of %d, which made %d retrievals", i + 1, made.size(), attempt.retrievals())
                .isEqualTo(!last && attempt.retrievals() > 0 ? 1 : 0);
        }
        assertThat(made.getLast().retrievals()).as("the last attempt retrieved the usable key").isPositive();
    }

    /**
     * What the application context does: the key selector bean makes the start-up retrieval, then the lifecycle
     * starts the retry. The retry's source is the same {@code idamJwkSource}, wrapped to record each attempt.
     */
    private void startUp() {
        jwkSource = configuration.idamJwkSource(telemetry);
        configuration.idamJwsKeySelector(jwkSource, telemetry);
        JWKSource<SecurityContext> recording = (selector, context) -> {
            int warnings = retryLogs.startingWith(NO_USABLE_KEY).size();
            int retrievalsBefore = retrievals();
            try {
                return jwkSource.get(selector, context);
            } finally {
                attempts.add(new Attempt(selector.getMatcher(), warnings, retrievals() - retrievalsBefore));
            }
        };
        retry = configuration.jwkSetStartupRetry(recording, telemetry);
        retry.start();
    }

    /**
     * Retrievals that passed the rate limiter: the health layer reports each one. WireMock's own count cannot be used
     * across a change of stub, which resets it.
     */
    private int retrievals() {
        return appInsights.eventsOfType(JwkSourceTelemetry.HEALTH).size();
    }

    /**
     * One background attempt.
     *
     * @param matcher         the matcher it selected with
     * @param warningsAtStart the no-usable-key WARNs logged before it started
     * @param retrievals      retrievals it made that passed the rate limiter
     */
    private record Attempt(JWKMatcher matcher, int warningsAtStart, int retrievals) {
    }

    private JwksProperties properties() {
        return new JwksProperties(
            jwksUri(wireMock),
            50,             // connect timeout, ms
            100,            // read timeout, ms
            51200,          // size limit, bytes
            3_000L,         // cache time to live
            320L,           // cache refresh timeout: above 2 attempts x (connect + read) = 300
            1_000L,         // refresh ahead time
            RATE_LIMIT_MS,
            30_000L,        // outage tolerance
            true);          // retrying, as in production
    }
}
