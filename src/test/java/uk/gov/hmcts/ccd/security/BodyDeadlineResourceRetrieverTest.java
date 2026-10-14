package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;
import static uk.gov.hmcts.ccd.security.JwkTestSupport.startWireMock;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CHUNKED;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CONTENT_LENGTH_CONNECTION_CLOSE;
import static uk.gov.hmcts.ccd.security.RawJwksServer.Framing.CONTENT_LENGTH_KEEP_ALIVE;
import static uk.gov.hmcts.ccd.security.RawJwksServer.headersAfter;
import static uk.gov.hmcts.ccd.security.RawJwksServer.lastChunkAfter;
import static uk.gov.hmcts.ccd.security.RawJwksServer.trickling;

/**
 * {@link BodyDeadlineResourceRetriever} against WireMock, for parity with Nimbus's {@link DefaultResourceRetriever},
 * and against {@link RawJwksServer}, which controls exactly when each byte is sent and sees when the client closes.
 */
class BodyDeadlineResourceRetrieverTest {

    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int READ_TIMEOUT_MS = 1_000;
    private static final int SIZE_LIMIT_BYTES = 51_200;
    private static final int NO_DEADLINE_MS = 30_000;
    private static final int DEADLINE_MS = 1_500;
    private static final long SLACK_MS = 1_000L;

    private WireMockServer wireMock;

    @BeforeEach
    void setUp() {
        wireMock = startWireMock();
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("returns a normal response exactly as Nimbus does: content byte for byte and content type")
    void normalResponseIsUnchanged() throws Exception {
        String body = originalJwkSet().replace("}", ", \"note\": \"café ✓\"}");
        wireMock.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/jwk-set+json; charset=UTF-8")
            .withBody(body.getBytes(StandardCharsets.UTF_8))));

        Resource resource = deadlineRetriever(NO_DEADLINE_MS).retrieveResource(url("/ok"));

        assertThat(resource.getContent()).isEqualTo(body);
        assertThat(resource.getContentType()).isEqualTo("application/jwk-set+json; charset=UTF-8");
        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), "/ok")).isEqualTo(outcome(nimbus(), "/ok"));
    }

    @Test
    @DisplayName("returns a chunked response sent within the deadline exactly as Nimbus does")
    void chunkedResponseWithinTheDeadlineIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/chunked")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(originalJwkSet())
            .withChunkedDribbleDelay(5, 200)));

        Outcome outcome = outcome(deadlineRetriever(DEADLINE_MS), "/chunked");

        assertThat(outcome.content()).isEqualTo(originalJwkSet());
        assertThat(outcome).isEqualTo(outcome(nimbus(), "/chunked"));
    }

    @Test
    @DisplayName("fails an error status with Nimbus's exception and message")
    void errorStatusIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/503")).willReturn(aResponse().withStatus(503).withBody("down")));
        wireMock.stubFor(get(urlEqualTo("/404")).willReturn(aResponse().withStatus(404)));

        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), "/503"))
            .isEqualTo(outcome(nimbus(), "/503"))
            .extracting(Outcome::error).asString().contains("503");
        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), "/404"))
            .isEqualTo(outcome(nimbus(), "/404"))
            .extracting(Outcome::error).asString().isNotEmpty();
    }

    @Test
    @DisplayName("enforces the size limit with Nimbus's message")
    void sizeLimitIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/big")).willReturn(aResponse()
            .withStatus(200)
            .withBody("x".repeat(SIZE_LIMIT_BYTES + 1))));

        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), "/big"))
            .isEqualTo(outcome(nimbus(), "/big"))
            .extracting(Outcome::error).asString()
            .isEqualTo("java.io.IOException: Exceeded configured input limit of " + SIZE_LIMIT_BYTES + " bytes");
    }

    @Test
    @DisplayName("follows a redirect as Nimbus does")
    void redirectIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/moved")).willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", "/ok")));
        wireMock.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(originalJwkSet())));

        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), "/moved"))
            .isEqualTo(outcome(nimbus(), "/moved"))
            .extracting(Outcome::content).isEqualTo(originalJwkSet());
    }

    @Test
    @DisplayName("keeps the read timeout's meaning and message: a server that sends nothing fails after it")
    void readTimeoutIsUnchanged() {
        wireMock.stubFor(get(urlEqualTo("/stalled")).willReturn(aResponse()
            .withStatus(200)
            .withBody(originalJwkSet())
            .withFixedDelay(READ_TIMEOUT_MS + 2_000)));

        long start = System.nanoTime();
        Outcome outcome = outcome(deadlineRetriever(NO_DEADLINE_MS), "/stalled");
        long elapsedMs = elapsedMs(start);

        assertThat(outcome).isEqualTo(outcome(nimbus(), "/stalled"));
        assertThat(outcome.error()).isEqualTo("java.net.SocketTimeoutException: Read timed out");
        assertThat(elapsedMs).isBetween((long) READ_TIMEOUT_MS, READ_TIMEOUT_MS + SLACK_MS);
    }

    @Test
    @DisplayName("fails a refused connection with Nimbus's exception and message")
    void refusedConnectionIsUnchanged() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        URL url = URI.create("http://localhost:" + closedPort + "/o/jwks").toURL();

        assertThat(outcome(deadlineRetriever(NO_DEADLINE_MS), url)).isEqualTo(outcome(nimbus(), url));
    }

    @Test
    @DisplayName("cuts off a trickling chunked body within deadline + read timeout, and closes the connection")
    void cutsOffATricklingChunkedBodyAndClosesTheConnection() throws Exception {
        try (RawJwksServer server = new RawJwksServer(trickling(CHUNKED))) {
            BodyDeadlineResourceRetriever retriever = deadlineRetriever(DEADLINE_MS);
            URL url = server.url();
            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");
            long elapsedMs = elapsedMs(start);

            assertThat(elapsedMs).isBetween((long) DEADLINE_MS, DEADLINE_MS + READ_TIMEOUT_MS + SLACK_MS);
            await().atMost(Duration.ofMillis(SLACK_MS))
                .untilAsserted(() -> assertThat(server.clientClosed()).as("the server saw the close").isTrue());
            assertThat(server.finishedSending()).as("it was cut off mid-body").isFalse();
        }
    }

    @Test
    @DisplayName("cuts off a trickling fixed-length body when the server is not keeping the connection alive, and "
        + "closes it")
    void cutsOffATricklingFixedLengthBodyAndClosesTheConnection() throws Exception {
        try (RawJwksServer server = new RawJwksServer(trickling(CONTENT_LENGTH_CONNECTION_CLOSE))) {
            BodyDeadlineResourceRetriever retriever = deadlineRetriever(DEADLINE_MS);
            URL url = server.url();
            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");

            assertThat(elapsedMs(start)).isBetween((long) DEADLINE_MS, DEADLINE_MS + READ_TIMEOUT_MS + SLACK_MS);
            await().atMost(Duration.ofMillis(SLACK_MS))
                .untilAsserted(() -> assertThat(server.clientClosed()).as("the server saw the close").isTrue());
        }
    }

    /**
     * The close behaviour in {@link BodyDeadlineResourceRetriever}'s Javadoc: on a keep-alive response with a
     * {@code Content-Length}, the JDK gives the socket to its keep-alive cleaner instead of closing it. The caller is
     * still released at the deadline. If a JDK upgrade changes this, update the Javadoc and the report.
     */
    @Test
    @DisplayName("cuts off a trickling keep-alive fixed-length body; the JDK drains that socket in the background")
    void cutsOffATricklingKeepAliveBodyButTheJdkKeepsTheSocket() throws Exception {
        try (RawJwksServer server = new RawJwksServer(trickling(CONTENT_LENGTH_KEEP_ALIVE))) {
            BodyDeadlineResourceRetriever retriever = deadlineRetriever(DEADLINE_MS);
            URL url = server.url();
            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");

            assertThat(elapsedMs(start)).isBetween((long) DEADLINE_MS, DEADLINE_MS + READ_TIMEOUT_MS + SLACK_MS);
            await().during(Duration.ofMillis(SLACK_MS)).atMost(Duration.ofMillis(2 * SLACK_MS))
                .until(() -> !server.clientClosed() && !server.finishedSending());
        }
    }

    @Test
    @DisplayName("checks the deadline after a skip, as after a read")
    void skipIsDeadlineChecked() throws Exception {
        try (RawJwksServer server = new RawJwksServer(trickling(CHUNKED))) {
            long start = System.nanoTime();
            HttpURLConnection connection = deadlineRetriever(DEADLINE_MS).openHTTPConnection(server.url());
            try {
                InputStream body = connection.getInputStream();
                assertThatThrownBy(() -> {
                    while (body.skip(1) > 0) {
                        // Skips the trickle a byte at a time, without reading it.
                    }
                }).hasMessage("IDAM JWK set retrieval exceeded its " + DEADLINE_MS + "ms deadline");

                assertThat(elapsedMs(start)).isBetween((long) DEADLINE_MS, DEADLINE_MS + READ_TIMEOUT_MS + SLACK_MS);
                assertThat(server.finishedSending()).as("it was cut off mid-body").isFalse();
            } finally {
                connection.disconnect();
            }
        }
    }

    /**
     * This class on its own. In production {@link TimeLimitedResourceRetriever} wraps it and releases the caller at the
     * deadline ({@code TimeLimitedResourceRetrieverTest}).
     */
    @Test
    @DisplayName("does not cut off slow headers, but fails at the first body read if they arrived after the deadline")
    void slowHeadersAreNotCutOffButCountTowardsTheDeadline() throws Exception {
        int headersAfterMs = 800;
        int deadlineMs = 300;
        try (RawJwksServer server = new RawJwksServer(headersAfter(headersAfterMs))) {
            BodyDeadlineResourceRetriever retriever = deadlineRetriever(deadlineMs);
            URL url = server.url();
            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .hasMessage("IDAM JWK set retrieval exceeded its " + deadlineMs + "ms deadline");

            assertThat(elapsedMs(start))
                .as("waited for the headers, as before, rather than giving up at the deadline")
                .isBetween((long) headersAfterMs, headersAfterMs + SLACK_MS);
        }
        try (RawJwksServer server = new RawJwksServer(headersAfter(100))) {
            assertThat(deadlineRetriever(deadlineMs).retrieveResource(server.url()).getContent())
                .as("headers inside the deadline")
                .isEqualTo(originalJwkSet());
        }
    }

    @Test
    @DisplayName("does not fail a read that reports the end of the body after the deadline")
    void endOfStreamAfterTheDeadlineIsNotAFailure() throws Exception {
        int deadlineMs = 300;
        try (RawJwksServer server = new RawJwksServer(lastChunkAfter(deadlineMs + 500))) {
            assertThat(deadlineRetriever(deadlineMs).retrieveResource(server.url()).getContent())
                .isEqualTo(originalJwkSet());
        }
    }

    private static BodyDeadlineResourceRetriever deadlineRetriever(int deadlineMs) {
        return new BodyDeadlineResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES, deadlineMs);
    }

    private static DefaultResourceRetriever nimbus() {
        return new DefaultResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES);
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

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private record Outcome(String content, String contentType, String error) {
    }
}
