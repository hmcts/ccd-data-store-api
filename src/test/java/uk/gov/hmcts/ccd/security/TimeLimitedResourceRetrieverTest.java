package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.RawJwksServer.healthy;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowChunkExtension;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowHeaders;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowTrailers;

/**
 * {@link TimeLimitedResourceRetriever} over {@link BodyDeadlineResourceRetriever}, as {@link JwkSourceConfiguration}
 * builds it, against {@link RawJwksServer}; and over stand-in retrievers for its own behaviour: the in-flight cap,
 * close and interrupts.
 */
class TimeLimitedResourceRetrieverTest {

    private static final int CONNECT_TIMEOUT_MS = 500;
    /** Far above the trickle interval, so no single read times out: only the deadline can end the request. */
    private static final int READ_TIMEOUT_MS = 1_000;
    private static final int SIZE_LIMIT_BYTES = 51_200;
    private static final int DEADLINE_MS = 200;
    /** For requests expected to succeed: room for a cold JVM on a loaded build. */
    private static final int GENEROUS_DEADLINE_MS = 5_000;
    /** Well under the 3s the server trickles for, and room for a loaded build. */
    private static final long MARGIN_MS = 1_000L;
    private static final String NOT_USED = "http://localhost/o/jwks";

    static Stream<Arguments> slowFraming() {
        return Stream.of(
            Arguments.of("chunk-size line extension", slowChunkExtension()),
            Arguments.of("response headers", slowHeaders()),
            Arguments.of("trailers", slowTrailers()));
    }

    @ParameterizedTest(name = "slow {0}: fails at the deadline, and the abandoned request closes its connection")
    @MethodSource("slowFraming")
    void slowFramingFailsAtTheDeadline(String name, RawJwksServer.Script script) throws Exception {
        try (RawJwksServer server = new RawJwksServer(script);
             TimeLimitedResourceRetriever retriever = retriever(DEADLINE_MS)) {
            URL url = server.url();
            long start = System.nanoTime();
            Throwable thrown = catchThrowable(() -> retriever.retrieveResource(url));
            long elapsedMs = elapsedMs(start);

            assertThat(thrown).as("slow %s, after %dms", name, elapsedMs)
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");
            assertThat(elapsedMs).isBetween((long) DEADLINE_MS, DEADLINE_MS + MARGIN_MS);
            await().atMost(Duration.ofMillis(MARGIN_MS))
                .untilAsserted(() -> assertThat(server.clientClosed()).as("the server saw the close").isTrue());
            assertThat(server.finishedSending()).as("it was cut off mid-response").isFalse();
        }
    }

    @Test
    @DisplayName("returns a normal response unchanged, on a virtual fetch thread that ends with it")
    void normalResponseIsUnchangedAndNoFetchThreadIsLeftRunning() throws Exception {
        AtomicReference<Thread> fetchThread = new AtomicReference<>();
        ResourceRetriever body = bodyDeadlineRetriever(GENEROUS_DEADLINE_MS);
        ResourceRetriever recording = url -> {
            fetchThread.set(Thread.currentThread());
            return body.retrieveResource(url);
        };
        try (RawJwksServer server = new RawJwksServer(healthy());
             TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(recording,
                 GENEROUS_DEADLINE_MS)) {
            Resource resource = retriever.retrieveResource(server.url());

            assertThat(resource.getContent()).isEqualTo(originalJwkSet());
            assertThat(resource.getContentType()).isEqualTo("application/json");
            assertThat(fetchThread.get().getName()).startsWith("idam-jwks-fetch-");
            assertThat(fetchThread.get().isVirtual()).isTrue();
            await().atMost(Duration.ofMillis(MARGIN_MS)).until(() -> !fetchThread.get().isAlive());
        }
    }

    @Test
    @DisplayName("rethrows the retrieval's own IOException and RuntimeException as they are")
    void rethrowsTheRetrievalsOwnExceptions() {
        IOException ioException = new IOException("Couldn't connect");
        IllegalStateException runtimeException = new IllegalStateException("bug");

        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(url -> {
            throw ioException;
        }, GENEROUS_DEADLINE_MS)) {
            assertThatThrownBy(() -> retriever.retrieveResource(url())).isSameAs(ioException);
        }
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(url -> {
            throw runtimeException;
        }, GENEROUS_DEADLINE_MS)) {
            assertThatThrownBy(() -> retriever.retrieveResource(url())).isSameAs(runtimeException);
        }
    }

    @Test
    @DisplayName("fails at once, without queueing, while two earlier retrievals are still running")
    void failsAtOnceWhileTwoRetrievalsAreStillRunning() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        ResourceRetriever stuck = url -> {
            awaitIgnoringInterrupts(release); // like a DNS lookup: an interrupt does not end it
            return new Resource(originalJwkSet(), "application/json");
        };
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(stuck, DEADLINE_MS)) {
            for (int i = 0; i < TimeLimitedResourceRetriever.MAX_IN_FLIGHT; i++) {
                assertThatThrownBy(() -> retriever.retrieveResource(url())).hasMessageContaining("deadline");
            }

            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url()))
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("IDAM JWK set retrieval not started: 2 earlier retrievals are still running past their "
                    + "deadline");
            assertThat(elapsedMs(start)).as("failed at once").isLessThan(DEADLINE_MS);

            release.countDown();
            await().atMost(Duration.ofMillis(MARGIN_MS)).ignoreExceptions()
                .until(() -> retriever.retrieveResource(url()).getContent().equals(originalJwkSet()));
        }
    }

    @Test
    @DisplayName("close() interrupts a retrieval in flight, and later retrievals fail at once")
    void closeStopsTheRetrievalsAndFailsLaterOnes() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        ResourceRetriever blocking = url -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
            throw new InterruptedIOException();
        };
        TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(blocking, GENEROUS_DEADLINE_MS);
        final Thread caller = Thread.ofPlatform().start(() -> catchThrowable(() -> retriever.retrieveResource(url())));
        assertThat(started.await(MARGIN_MS, TimeUnit.MILLISECONDS)).isTrue();

        retriever.close();

        await().atMost(Duration.ofMillis(MARGIN_MS)).untilTrue(interrupted);
        assertThat(caller.join(Duration.ofMillis(MARGIN_MS))).as("the caller was released").isTrue();
        long start = System.nanoTime();
        assertThatThrownBy(() -> retriever.retrieveResource(url()))
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("IDAM JWK set retriever is closed");
        assertThat(elapsedMs(start)).as("failed at once").isLessThan(DEADLINE_MS);
    }

    @Test
    @DisplayName("an interrupted caller gets InterruptedIOException, keeps its interrupt flag, and stops the retrieval")
    void interruptedCallerStopsTheRetrieval() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        ResourceRetriever blocking = url -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
            throw new InterruptedIOException();
        };
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(blocking,
            GENEROUS_DEADLINE_MS)) {
            Thread caller = Thread.currentThread();
            Thread.ofPlatform().start(() -> {
                try {
                    started.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                caller.interrupt();
            });

            Throwable thrown = catchThrowable(() -> retriever.retrieveResource(url()));

            assertThat(Thread.interrupted()).as("the interrupt flag was kept").isTrue();
            assertThat(thrown).isExactlyInstanceOf(InterruptedIOException.class);
            await().atMost(Duration.ofMillis(MARGIN_MS)).untilTrue(interrupted);
        }
    }

    private static TimeLimitedResourceRetriever retriever(int deadlineMs) {
        return new TimeLimitedResourceRetriever(bodyDeadlineRetriever(deadlineMs), deadlineMs);
    }

    private static BodyDeadlineResourceRetriever bodyDeadlineRetriever(int deadlineMs) {
        return new BodyDeadlineResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES, deadlineMs);
    }

    private static URL url() throws IOException {
        return URI.create(NOT_USED).toURL();
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (latch.getCount() > 0) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
