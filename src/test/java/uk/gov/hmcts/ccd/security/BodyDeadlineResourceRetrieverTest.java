package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
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

/**
 * {@link BodyDeadlineResourceRetriever} against WireMock, for parity with Nimbus's {@link DefaultResourceRetriever},
 * and against a raw socket server, which controls exactly when each byte is sent and sees when the client closes.
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
        try (RawServer server = RawServer.trickling(Framing.CHUNKED)) {
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
        try (RawServer server = RawServer.trickling(Framing.CONTENT_LENGTH_CONNECTION_CLOSE)) {
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
     * The known limit in {@link BodyDeadlineResourceRetriever}'s Javadoc: on a keep-alive response with a
     * {@code Content-Length}, the JDK gives the socket to its keep-alive cleaner instead of closing it. The caller is
     * still released at the deadline. If a JDK upgrade changes this, update the Javadoc and the report.
     */
    @Test
    @DisplayName("cuts off a trickling keep-alive fixed-length body; the JDK drains that socket in the background")
    void cutsOffATricklingKeepAliveBodyButTheJdkKeepsTheSocket() throws Exception {
        try (RawServer server = RawServer.trickling(Framing.CONTENT_LENGTH_KEEP_ALIVE)) {
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
        try (RawServer server = RawServer.trickling(Framing.CHUNKED)) {
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

    @Test
    @DisplayName("does not cut off slow headers, but fails at the first body read if they arrived after the deadline")
    void slowHeadersAreNotCutOffButCountTowardsTheDeadline() throws Exception {
        int headersAfterMs = 800;
        int deadlineMs = 300;
        try (RawServer server = RawServer.headersAfter(headersAfterMs)) {
            BodyDeadlineResourceRetriever retriever = deadlineRetriever(deadlineMs);
            URL url = server.url();
            long start = System.nanoTime();
            assertThatThrownBy(() -> retriever.retrieveResource(url))
                .hasMessage("IDAM JWK set retrieval exceeded its " + deadlineMs + "ms deadline");

            assertThat(elapsedMs(start))
                .as("waited for the headers, as before, rather than giving up at the deadline")
                .isBetween((long) headersAfterMs, headersAfterMs + SLACK_MS);
        }
        try (RawServer server = RawServer.headersAfter(100)) {
            assertThat(deadlineRetriever(deadlineMs).retrieveResource(server.url()).getContent())
                .as("headers inside the deadline")
                .isEqualTo(originalJwkSet());
        }
    }

    @Test
    @DisplayName("does not fail a read that reports the end of the body after the deadline")
    void endOfStreamAfterTheDeadlineIsNotAFailure() throws Exception {
        int deadlineMs = 300;
        try (RawServer server = RawServer.lastChunkAfter(deadlineMs + 500)) {
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

    private enum Framing {
        CHUNKED,
        CONTENT_LENGTH_KEEP_ALIVE,
        CONTENT_LENGTH_CONNECTION_CLOSE
    }

    /**
     * Serves one request, sending each part of the response when its script says, and records when the client closes
     * the connection: a read on the server side returns end of stream or fails.
     */
    private static final class RawServer implements Closeable {

        private static final int PIECES = 40;
        private static final int PIECE_INTERVAL_MS = 200;

        private final ServerSocket serverSocket;
        private final Thread thread;
        private volatile boolean clientClosed;
        private volatile boolean finishedSending;

        private RawServer(Script script) throws IOException {
            serverSocket = new ServerSocket(0);
            thread = new Thread(() -> serve(script), "raw-jwks-server");
            thread.setDaemon(true);
            thread.start();
        }

        static RawServer trickling(Framing framing) throws IOException {
            return new RawServer(out -> {
                StringBuilder headers = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n");
                if (framing == Framing.CHUNKED) {
                    headers.append("Transfer-Encoding: chunked\r\n");
                } else {
                    headers.append("Content-Length: ").append(PIECES).append("\r\n");
                }
                if (framing == Framing.CONTENT_LENGTH_CONNECTION_CLOSE) {
                    headers.append("Connection: close\r\n");
                }
                send(out, headers.append("\r\n").toString());
                for (int i = 0; i < PIECES; i++) {
                    Thread.sleep(PIECE_INTERVAL_MS); // NOSONAR - the server trickles the body; not a wait
                    send(out, framing == Framing.CHUNKED ? "1\r\nx\r\n" : "x");
                }
                if (framing == Framing.CHUNKED) {
                    send(out, "0\r\n\r\n");
                }
            });
        }

        static RawServer headersAfter(int delayMs) throws IOException {
            return new RawServer(out -> {
                Thread.sleep(delayMs); // NOSONAR - the server sends its headers late; not a wait
                byte[] body = originalJwkSet().getBytes(StandardCharsets.UTF_8);
                send(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length
                    + "\r\nConnection: close\r\n\r\n" + originalJwkSet());
            });
        }

        static RawServer lastChunkAfter(int delayMs) throws IOException {
            return new RawServer(out -> {
                byte[] body = originalJwkSet().getBytes(StandardCharsets.UTF_8);
                send(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nTransfer-Encoding: chunked\r\n\r\n"
                    + Integer.toHexString(body.length) + "\r\n" + originalJwkSet() + "\r\n");
                Thread.sleep(delayMs); // NOSONAR - the server sends its last chunk late; not a wait
                send(out, "0\r\n\r\n");
            });
        }

        URL url() throws IOException {
            return URI.create("http://localhost:" + serverSocket.getLocalPort() + "/o/jwks").toURL();
        }

        boolean clientClosed() {
            return clientClosed;
        }

        boolean finishedSending() {
            return finishedSending;
        }

        @Override
        public void close() throws IOException {
            thread.interrupt();
            serverSocket.close();
        }

        private void serve(Script script) {
            try (Socket socket = serverSocket.accept()) {
                InputStream in = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
                String line = reader.readLine();
                while (line != null && !line.isEmpty()) {
                    line = reader.readLine();
                }
                Thread watcher = createWatcher(in);
                script.respond(socket.getOutputStream());
                finishedSending = true;
                watcher.join();
            } catch (IOException | InterruptedException e) {
                // The client went away mid-response, or the test is over.
            }
        }

        private @NonNull Thread createWatcher(InputStream in) {
            Thread watcher = new Thread(() -> {
                try {
                    while (in.read() != -1) {
                        // A GET has nobody; anything else is ignored.
                    }
                } catch (IOException e) {
                    // Reset by the client: closed too.
                }
                clientClosed = true;
            }, "raw-jwks-server-close-watcher");
            watcher.setDaemon(true);
            watcher.start();
            return watcher;
        }

        private static void send(OutputStream out, String data) throws IOException {
            out.write(data.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        @FunctionalInterface
        private interface Script {
            void respond(OutputStream out) throws IOException, InterruptedException;
        }
    }
}
