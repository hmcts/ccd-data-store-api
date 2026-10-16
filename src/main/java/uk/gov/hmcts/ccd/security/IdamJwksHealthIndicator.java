package uk.gov.hmcts.ccd.security;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Keeps an instance out of service until it has retrieved a usable IDAM signing key at least once. Registered as
 * {@code idamJwks}.
 *
 * <p>An instance that starts while IDAM is unreachable falls back at start-up (see {@link JwkSourceConfiguration})
 * with nothing in its key cache, so it would fail every authenticated request with a 500 until retrieval succeeds.
 * Before CCD-8077 such an instance did not start at all and never took traffic; this keeps it that way, without
 * stopping it from starting.
 *
 * <p>A key set that IDAM returns with no usable signing key ({@link JwkSourceConfiguration#USABLE_SIGNING_KEYS}) does
 * not count: it verifies no token either, and master failed start-up on it.
 *
 * <p>The indicator latches. It is DOWN until {@link JwkSourceTelemetry} sees the first key set with a usable signing
 * key, and UP for the rest of the JVM's life after that. It must not follow IDAM's health once UP: an IDAM outage
 * would then take every instance out of service at once, including those still serving cached keys within the outage
 * tolerance window.
 *
 * <p>It belongs in the readiness group only. In liveness, it would fail the start-up probe, which checks liveness, so
 * an instance waiting for IDAM would be restarted.
 *
 * <p>It does no retrieval itself, because retrieval can take longer than a probe waits for an answer.
 * {@link JwkSetStartupRetry} retries in the background instead.
 */
public class IdamJwksHealthIndicator implements HealthIndicator {

    private static final Health UP = Health.up().build();

    private static final Health DOWN = Health.down()
        .withDetail("reason", "No usable IDAM signing key retrieved since start-up")
        .build();

    private final JwkSourceTelemetry telemetry;

    public IdamJwksHealthIndicator(JwkSourceTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public Health health() {
        return telemetry.hasRetrievedUsableSigningKey() ? UP : DOWN;
    }
}
