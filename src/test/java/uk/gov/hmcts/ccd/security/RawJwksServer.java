package uk.gov.hmcts.ccd.security;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import static uk.gov.hmcts.ccd.security.JwkTestSupport.originalJwkSet;

/**
 * Answers each connection with the current {@link Script}, sending each part of the response when the script says,
 * which WireMock cannot do for the response headers or the chunk framing. Records when the client closes the latest
 * connection: a read on the server side returns end of stream or fails. Over HTTP, or over HTTPS with
 * {@link #overTls}, where each send is its own TLS record, flushed when the script says.
 */
final class RawJwksServer implements Closeable {

    /** One byte every 100ms, by default 30 bytes: 3s in all. Each byte is well inside the tests' read timeouts. */
    static final int TRICKLE_BYTES = 30;
    static final int TRICKLE_INTERVAL_MS = 100;

    private static final String LOOPBACK = "127.0.0.1";
    private static final int PIECES = 40;
    private static final int PIECE_INTERVAL_MS = 200;

    private final ServerSocket serverSocket;
    private final List<Thread> threads = new CopyOnWriteArrayList<>();
    private final AtomicInteger requests = new AtomicInteger();
    private volatile Script script;
    private volatile boolean clientClosed;
    private volatile boolean finishedSending;

    RawJwksServer(Script script) throws IOException {
        this(script, ServerSocketFactory.getDefault());
    }

    private RawJwksServer(Script script, ServerSocketFactory sockets) throws IOException {
        this.script = script;
        // Bound to 127.0.0.1, not the wildcard, so a server another fork binds to the same port number with
        // SO_REUSEADDR (WireMock's Jetty does) cannot answer this server's requests.
        serverSocket = sockets.createServerSocket(0, 50, InetAddress.getByName(LOOPBACK));
        start(new Thread(this::acceptConnections, "raw-jwks-server"));
    }

    /** Serves HTTPS with the given server context; the handshake runs on the first read of the request. */
    static RawJwksServer overTls(Script script, SSLContext tls) throws IOException {
        return new RawJwksServer(script, tls.getServerSocketFactory());
    }

    enum Framing {
        CHUNKED,
        CONTENT_LENGTH_KEEP_ALIVE,
        CONTENT_LENGTH_CONNECTION_CLOSE
    }

    /** The original key set, sent at once, closing the connection after it. */
    static Script healthy() {
        return out -> {
            byte[] body = originalJwkSet().getBytes(StandardCharsets.UTF_8);
            send(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n" + originalJwkSet());
        };
    }

    /** Headers at once, then a 40-byte body, one byte every 200ms. */
    static Script trickling(Framing framing) {
        return out -> {
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
        };
    }

    /** The status line at once, then a header trickled a byte at a time, then the rest of a healthy response. */
    static Script slowHeaders() {
        return out -> {
            send(out, "HTTP/1.1 200 OK\r\nX-Slow: ");
            trickle(out, TRICKLE_BYTES);
            send(out, "\r\n");
            byte[] body = originalJwkSet().getBytes(StandardCharsets.UTF_8);
            send(out, "Content-Type: application/json\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n" + originalJwkSet());
        };
    }

    /**
     * Chunked headers at once, then the key set's chunk-size line with an extension trickled a byte at a time. The
     * JDK's {@code ChunkedInputStream} reads the whole line inside one {@code read()}, which returns no data until the
     * line ends.
     */
    static Script slowChunkExtension() {
        return slowChunkExtension(TRICKLE_BYTES);
    }

    static Script slowChunkExtension(int bytes) {
        return out -> {
            send(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nTransfer-Encoding: chunked\r\n\r\n");
            send(out, Integer.toHexString(originalJwkSet().getBytes(StandardCharsets.UTF_8).length) + ";x=");
            trickle(out, bytes);
            send(out, "\r\n" + originalJwkSet() + "\r\n0\r\n\r\n");
        };
    }

    /** The whole key set at once, then the last chunk with a trailer trickled a byte at a time. */
    static Script slowTrailers() {
        return out -> {
            sendChunkedHeadersAndKeySet(out);
            send(out, "0\r\nX-Slow: ");
            trickle(out, TRICKLE_BYTES);
            send(out, "\r\n\r\n");
        };
    }

    void respondWith(Script script) {
        this.script = script;
    }

    URL url() throws IOException {
        return URI.create(uri()).toURL();
    }

    String uri() {
        String scheme = serverSocket instanceof SSLServerSocket ? "https" : "http";
        return scheme + "://" + LOOPBACK + ":" + serverSocket.getLocalPort() + JwkTestSupport.JWKS_PATH;
    }

    int requests() {
        return requests.get();
    }

    boolean clientClosed() {
        return clientClosed;
    }

    boolean finishedSending() {
        return finishedSending;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        threads.forEach(Thread::interrupt);
    }

    private void acceptConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                start(new Thread(() -> serve(socket, script), "raw-jwks-server-connection"));
            } catch (IOException e) {
                // Closed: the test is over.
            }
        }
    }

    private void serve(Socket connection, Script script) {
        try (Socket socket = connection) {
            socket.setTcpNoDelay(true); // Each write goes out when the script says, not batched by Nagle.
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
            String line = reader.readLine();
            while (line != null && !line.isEmpty()) {
                line = reader.readLine();
            }
            requests.incrementAndGet();
            clientClosed = false;
            finishedSending = false;
            Thread watcher = start(new Thread(() -> watchForClose(in), "raw-jwks-server-close-watcher"));
            script.respond(socket.getOutputStream());
            finishedSending = true;
            watcher.join();
        } catch (IOException | InterruptedException e) {
            // The client went away mid-response, or the test is over.
        }
    }

    private void watchForClose(InputStream in) {
        try {
            while (in.read() != -1) {
                // A GET has no body; anything else is ignored.
            }
        } catch (IOException e) {
            // Reset by the client, or an SSLException over TLS: closed too.
        }
        clientClosed = true;
    }

    private Thread start(Thread thread) {
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
        return thread;
    }

    private static void sendChunkedHeadersAndKeySet(OutputStream out) throws IOException {
        byte[] body = originalJwkSet().getBytes(StandardCharsets.UTF_8);
        send(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nTransfer-Encoding: chunked\r\n\r\n"
            + Integer.toHexString(body.length) + "\r\n" + originalJwkSet() + "\r\n");
    }

    private static void trickle(OutputStream out, int bytes) throws IOException, InterruptedException {
        for (int i = 0; i < bytes; i++) {
            Thread.sleep(TRICKLE_INTERVAL_MS); // NOSONAR - the server trickles; not a wait
            send(out, "a");
        }
    }

    private static void send(OutputStream out, String data) throws IOException {
        out.write(data.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @FunctionalInterface
    interface Script {
        void respond(OutputStream out) throws IOException, InterruptedException;
    }
}
