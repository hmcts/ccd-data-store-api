package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CHUNKED;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CONTENT_LENGTH_CONNECTION_CLOSE;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CONTENT_LENGTH_KEEP_ALIVE;
import static uk.gov.hmcts.ccd.security.RawJwksServer.healthy;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowChunkExtension;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowHeaders;
import static uk.gov.hmcts.ccd.security.RawJwksServer.slowTrailers;
import static uk.gov.hmcts.ccd.security.RawJwksServer.trickling;
import static uk.gov.hmcts.ccd.security.TimeLimitedResourceRetrieverTest.Scheme.HTTP;
import static uk.gov.hmcts.ccd.security.TimeLimitedResourceRetrieverTest.Scheme.HTTPS;

/**
 * {@link TimeLimitedResourceRetriever} over Nimbus's {@link DefaultResourceRetriever}, as
 * {@link JwkSourceConfiguration} builds it: against {@link RawJwksServer}, which controls exactly when each byte is
 * sent and sees when the client closes, over HTTP and, as IDAM is called, HTTPS; and against WireMock for parity with
 * Nimbus on its own. Also over stand-in retrievers for its own behaviour: the in-flight cap, close and interrupts.
 */
class TimeLimitedResourceRetrieverTest {

    private static final int CONNECT_TIMEOUT_MS = 500;
    /** Far above the trickle interval, so no single read times out: only the deadline can end the request. */
    private static final int READ_TIMEOUT_MS = 1_000;
    private static final int SIZE_LIMIT_BYTES = 51_200;
    private static final int DEADLINE_MS = 200;
    /** For requests expected to succeed: room for a cold JVM on a loaded build. */
    private static final int GENEROUS_DEADLINE_MS = 5_000;
    /** Well under the 3s or more the server trickles for, and room for a loaded build. */
    private static final long MARGIN_MS = 1_000L;
    private static final String NOT_USED = "http://localhost/o/jwks";
    /**
     * A self-signed certificate for localhost and 127.0.0.1, valid until 2126, with its key. Made with:
     * <pre>
     * keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 36500 \
     *     -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1" -storetype PKCS12 \
     *     -keystore src/test/resources/security/localhost-tls.p12 -storepass changeit
     * </pre>
     */
    private static final String TLS_KEYSTORE = "/security/localhost-tls.p12";
    private static final String TLS_KEY_ALIAS = "localhost";
    private static final char[] TLS_KEYSTORE_PASSWORD = "changeit".toCharArray();

    /** The server's key and certificate. */
    private static SSLContext serverTls;
    /**
     * Trusts only the test certificate. Passed to Nimbus, so no global TLS state changes, and the default hostname
     * verification still runs.
     */
    private static SSLSocketFactory clientTls;

    private WireMockServer wireMock;

    enum Scheme {
        HTTP,
        HTTPS
    }

    @BeforeAll
    static void setUpTls() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = TimeLimitedResourceRetrieverTest.class.getResourceAsStream(TLS_KEYSTORE)) {
            keyStore.load(in, TLS_KEYSTORE_PASSWORD);
        }
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(keyStore, TLS_KEYSTORE_PASSWORD);
        serverTls = SSLContext.getInstance("TLSv1.3");
        serverTls.init(keys.getKeyManagers(), null, null);

        KeyStore trusted = KeyStore.getInstance("PKCS12");
        trusted.load(null, null);
        trusted.setCertificateEntry(TLS_KEY_ALIAS, keyStore.getCertificate(TLS_KEY_ALIAS));
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trusted);
        SSLContext client = SSLContext.getInstance("TLSv1.3");
        client.init(null, trust.getTrustManagers(), null);
        clientTls = client.getSocketFactory();

        // One handshake first, so a cold JSSE does not spend a timed test's 200ms deadline on it, cutting the request
        // off before the server reads it.
        try (RawJwksServer server = RawJwksServer.overTls(healthy(), serverTls)) {
            nimbus(HTTPS).retrieveResource(server.url());
        }
    }

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    static Stream<Arguments> slowResponses() {
        return Stream.of(
            Arguments.of(HTTP, "chunked body", trickling(CHUNKED)),
            Arguments.of(HTTP, "Content-Length body on a keep-alive connection", trickling(CONTENT_LENGTH_KEEP_ALIVE)),
            Arguments.of(HTTP, "Content-Length body with Connection: close",
                trickling(CONTENT_LENGTH_CONNECTION_CLOSE)),
            Arguments.of(HTTP, "response headers", slowHeaders()),
            Arguments.of(HTTP, "chunk-size line extension", slowChunkExtension()),
            Arguments.of(HTTP, "trailers", slowTrailers()),
            Arguments.of(HTTPS, "chunked body", trickling(CHUNKED)),
            Arguments.of(HTTPS, "response headers", slowHeaders()),
            Arguments.of(HTTPS, "chunk-size line extension", slowChunkExtension()),
            Arguments.of(HTTPS, "trailers", slowTrailers()));
    }

    @ParameterizedTest(name = "{0}, slow {1}: fails at the deadline, and the abandoned request closes its connection")
    @MethodSource("slowResponses")
    void slowResponseFailsAtTheDeadline(Scheme scheme, String name, RawJwksServer.Script script) throws IOException {
        try (RawJwksServer server = server(scheme, script);
             TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(nimbus(scheme), DEADLINE_MS)) {
            URL url = server.url();
            assertThat(url.getProtocol()).isEqualTo(scheme.name().toLowerCase(Locale.ROOT));
            long start = System.nanoTime();
            Throwable thrown = catchThrowable(() -> retriever.retrieveResource(url));
            long elapsedMs = elapsedMs(start);

            assertThat(thrown).as("%s, slow %s, after %dms", scheme, name, elapsedMs)
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");
            assertThat(elapsedMs).isBetween((long) DEADLINE_MS, DEADLINE_MS + MARGIN_MS);
            assertThat(server.requests()).as("the request reached the server").isEqualTo(1);
            await().atMost(Duration.ofMillis(MARGIN_MS))
                .untilAsserted(() -> assertThat(server.clientClosed()).as("the server saw the close").isTrue());
            assertThat(server.finishedSending()).as("it was cut off mid-response").isFalse();
        }
    }

    @Test
    @DisplayName("returns a normal response exactly as Nimbus does, on a virtual fetch thread that ends with it")
    void normalResponseIsUnchangedAndNoFetchThreadIsLeftRunning() throws IOException {
        String body = originalJwkSet().replace("}", ", \"note\": \"café ✓\"}");
        wireMock.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/jwk-set+json; charset=UTF-8")
            .withBody(body.getBytes(StandardCharsets.UTF_8))));
        AtomicReference<Thread> fetchThread = new AtomicReference<>();
        ResourceRetriever nimbus = nimbus();
        ResourceRetriever recording = url -> {
            fetchThread.set(Thread.currentThread());
            return nimbus.retrieveResource(url);
        };
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(recording,
            GENEROUS_DEADLINE_MS)) {
            Resource resource = retriever.retrieveResource(url("/ok"));

            assertThat(resource.getContent()).isEqualTo(body);
            assertThat(resource.getContentType()).isEqualTo("application/jwk-set+json; charset=UTF-8");
            assertThat(outcome(retriever, "/ok")).isEqualTo(outcome(nimbus, "/ok"));
            assertThat(fetchThread.get().getName()).startsWith("idam-jwks-fetch-");
            assertThat(fetchThread.get().isVirtual()).isTrue();
            await().atMost(Duration.ofMillis(MARGIN_MS)).until(() -> !fetchThread.get().isAlive());
        }
    }

    @Test
    @DisplayName("returns a normal HTTPS response exactly as Nimbus does")
    void normalHttpsResponseIsUnchanged() throws IOException {
        try (RawJwksServer server = RawJwksServer.overTls(healthy(), serverTls);
             TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(nimbus(HTTPS),
                 GENEROUS_DEADLINE_MS)) {
            URL url = server.url();
            assertThat(url.getProtocol()).isEqualTo("https");

            Outcome outcome = outcome(retriever, url);

            assertThat(outcome).isEqualTo(new Outcome(originalJwkSet(), "application/json", null));
            assertThat(outcome).isEqualTo(outcome(nimbus(HTTPS), url));
        }
    }

    @Test
    @DisplayName("returns a chunked response sent within the deadline exactly as Nimbus does")
    void chunkedResponseWithinTheDeadlineIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/chunked")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(originalJwkSet())
            .withChunkedDribbleDelay(5, 200)));

        try (TimeLimitedResourceRetriever retriever = retriever(GENEROUS_DEADLINE_MS)) {
            Outcome outcome = outcome(retriever, "/chunked");

            assertThat(outcome.content()).isEqualTo(originalJwkSet());
            assertThat(outcome).isEqualTo(outcome(nimbus(), "/chunked"));
        }
    }

    @Test
    @DisplayName("fails an error status with Nimbus's exception and message")
    void errorStatusIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/503")).willReturn(aResponse().withStatus(503).withBody("down")));
        wireMock.stubFor(get(urlEqualTo("/404")).willReturn(aResponse().withStatus(404)));

        try (TimeLimitedResourceRetriever retriever = retriever(GENEROUS_DEADLINE_MS)) {
            assertThat(outcome(retriever, "/503"))
                .isEqualTo(outcome(nimbus(), "/503"))
                .extracting(Outcome::error).asString()
                .startsWith("java.io.IOException: ").contains("503");
            assertThat(outcome(retriever, "/404"))
                .isEqualTo(outcome(nimbus(), "/404"))
                .extracting(Outcome::error).asString().isNotEmpty();
        }
    }

    @Test
    @DisplayName("enforces Nimbus's size limit with Nimbus's message")
    void sizeLimitIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/big")).willReturn(aResponse()
            .withStatus(200)
            .withBody("x".repeat(SIZE_LIMIT_BYTES + 1))));

        try (TimeLimitedResourceRetriever retriever = retriever(GENEROUS_DEADLINE_MS)) {
            assertThat(outcome(retriever, "/big"))
                .isEqualTo(outcome(nimbus(), "/big"))
                .extracting(Outcome::error).asString()
                .isEqualTo("java.io.IOException: Exceeded configured input limit of " + SIZE_LIMIT_BYTES + " bytes");
        }
    }

    @Test
    @DisplayName("rethrows the retrieval's own IOException and RuntimeException as they are")
    void rethrowsTheRetrievalsOwnExceptions() throws IOException {
        IOException ioException = new IOException("Couldn't connect");
        IllegalStateException runtimeException = new IllegalStateException("bug");
        URL url = url();

        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(unused -> {
            throw ioException;
        }, GENEROUS_DEADLINE_MS)) {
            assertThatThrownBy(() -> retriever.retrieveResource(url)).isSameAs(ioException);
        }
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(unused -> {
            throw runtimeException;
        }, GENEROUS_DEADLINE_MS)) {
            assertThatThrownBy(() -> retriever.retrieveResource(url)).isSameAs(runtimeException);
        }
    }

    @Test
    @DisplayName("fails at once, without queueing, while two earlier retrievals are still running")
    // A separate thread, so the test fails at 10s even when the stuck retrieval, which ignores interrupts, runs on
    // the test's own thread (if the wrapper stops running it on a fetch thread).
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void failsAtOnceWhileTwoRetrievalsAreStillRunning() throws IOException {
        CountDownLatch release = new CountDownLatch(1);
        ResourceRetriever stuck = url -> {
            awaitIgnoringInterrupts(release); // like a DNS lookup: an interrupt does not end it
            return new Resource(originalJwkSet(), "application/json");
        };
        try (TimeLimitedResourceRetriever retriever = new TimeLimitedResourceRetriever(stuck, DEADLINE_MS)) {
            URL url = url();
            for (int i = 0; i < TimeLimitedResourceRetriever.MAX_IN_FLIGHT; i++) {
                assertThatThrownBy(() -> retriever.retrieveResource(url)).hasMessageContaining("deadline");
            }

            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("IDAM JWK set retrieval not started: 2 earlier retrievals are still running past their "
                    + "deadline");
            assertThat(elapsedMs(start)).as("failed at once").isLessThan(DEADLINE_MS);

            release.countDown();
            await().atMost(Duration.ofMillis(MARGIN_MS)).ignoreExceptions()
                .until(() -> retriever.retrieveResource(url).getContent().equals(originalJwkSet()));
        }
    }

    @Test
    @DisplayName("close() interrupts a retrieval in flight, and later retrievals fail at once")
    void closeStopsTheRetrievalsAndFailsLaterOnes() throws IOException, InterruptedException {
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
        URL url = url();
        final Thread caller = Thread.ofPlatform().start(() -> catchThrowable(() -> retriever.retrieveResource(url)));
        assertThat(started.await(MARGIN_MS, TimeUnit.MILLISECONDS)).isTrue();

        retriever.close();

        await().atMost(Duration.ofMillis(MARGIN_MS)).untilTrue(interrupted);
        assertThat(caller.join(Duration.ofMillis(MARGIN_MS))).as("the caller was released").isTrue();
        long start = System.nanoTime();
        assertThatThrownBy(() -> retriever.retrieveResource(url))
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("IDAM JWK set retriever is closed");
        assertThat(elapsedMs(start)).as("failed at once").isLessThan(DEADLINE_MS);
    }

    @Test
    @DisplayName("an interrupted caller gets InterruptedIOException, keeps its interrupt flag, and stops the retrieval")
    void interruptedCallerStopsTheRetrieval() throws IOException {
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
        URL url = url();
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

            Throwable thrown = catchThrowable(() -> retriever.retrieveResource(url));

            assertThat(Thread.interrupted()).as("the interrupt flag was kept").isTrue();
            assertThat(thrown).isExactlyInstanceOf(InterruptedIOException.class);
            await().atMost(Duration.ofMillis(MARGIN_MS)).untilTrue(interrupted);
        }
    }

    // As JwkSourceConfiguration builds it.
    private static TimeLimitedResourceRetriever retriever(int deadlineMs) {
        return new TimeLimitedResourceRetriever(nimbus(), deadlineMs);
    }

    private static DefaultResourceRetriever nimbus() {
        return new DefaultResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES);
    }

    // The same, with the test trust for HTTPS: disconnectAfterUse is true, as in the three-argument constructor.
    private static DefaultResourceRetriever nimbus(Scheme scheme) {
        return scheme == HTTPS
            ? new DefaultResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES, true, clientTls)
            : nimbus();
    }

    private static RawJwksServer server(Scheme scheme, RawJwksServer.Script script) throws IOException {
        return scheme == HTTPS ? RawJwksServer.overTls(script, serverTls) : new RawJwksServer(script);
    }

    private static URL url() throws IOException {
        return URI.create(NOT_USED).toURL();
    }

    private URL url(String path) throws IOException {
        return URI.create(wireMock.baseUrl() + path).toURL();
    }

    private Outcome outcome(ResourceRetriever retriever, String path) {
        try {
            return outcome(retriever, url(path));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Outcome outcome(ResourceRetriever retriever, URL url) {
        try {
            Resource resource = retriever.retrieveResource(url);
            return new Outcome(resource.getContent(), resource.getContentType(), null);
        } catch (IOException e) {
            return new Outcome(null, null, e.toString().replace(url.toString(), "<url>"));
        }
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

    private record Outcome(String content, String contentType, String error) {
    }
}
