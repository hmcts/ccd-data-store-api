package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.CapturedLogs;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.RecordingAppInsights;

/**
 * What one background attempt does when the JWK source throws something other than a {@link KeySourceException}.
 * The scheduled executor drops a periodic task whose run throws, silently, so the retry must not let anything
 * escape.
 */
class JwkSetStartupRetryTest {

    private static final long INTERVAL_MS = 50L;
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(5);

    private final AtomicInteger calls = new AtomicInteger();
    private JwkSetStartupRetry retry;
    private CapturedLogs retryLogs;

    @BeforeEach
    void setUp() {
        retryLogs = new CapturedLogs(JwkSetStartupRetry.class);
    }

    @AfterEach
    void tearDown() {
        if (retry != null) {
            retry.stop();
        }
        retryLogs.close();
    }

    @Test
    @DisplayName("keeps retrying after an Error, and logs the first one at WARN")
    void keepsRetryingAfterAnError() {
        start((selector, context) -> {
            int call = calls.incrementAndGet();
            if (call == 1 || call == 3) {
                throw new NoClassDefFoundError("simulated");
            }
            throw new KeySourceException("down");
        });

        await().atMost(SETTLE_TIMEOUT).until(() -> calls.get() >= 4);
        assertThat(retry.isRunning()).isTrue();

        assertThat(retryLogs.startingWith("Background IDAM JWK set retrieval threw"))
            .as("the first Error only")
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).isEqualTo("Background IDAM JWK set retrieval threw "
                    + "java.lang.NoClassDefFoundError: simulated; still retrying every " + INTERVAL_MS + "ms");
            });
        assertThat(retryLogs.startingWith("").stream()
            .filter(event -> !event.getFormattedMessage().startsWith("Background IDAM JWK set retrieval threw")))
            .as("everything else, the second Error included")
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    }

    @Test
    @DisplayName("keeps retrying after a RuntimeException, logging at DEBUG only")
    void keepsRetryingAfterARuntimeException() {
        start((selector, context) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("simulated");
        });

        await().atMost(SETTLE_TIMEOUT).until(() -> calls.get() >= 3);
        assertThat(retry.isRunning()).isTrue();

        assertThat(retryLogs.startingWith(""))
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
        assertThat(retryLogs.startingWith("Background IDAM JWK set retrieval failed; retrying in " + INTERVAL_MS
            + "ms: java.lang.IllegalStateException: simulated")).isNotEmpty();
    }

    private void start(JWKSource<SecurityContext> source) {
        retry = new JwkSetStartupRetry(source, new JwkSourceTelemetry(new RecordingAppInsights()), INTERVAL_MS);
        retry.start();
    }
}
