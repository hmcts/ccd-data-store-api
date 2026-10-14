package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.util.DefaultResourceRetriever;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Nimbus's {@link DefaultResourceRetriever}, unchanged, plus a per-request deadline checked as the response body is
 * read.
 *
 * <p>Connect and read timeouts bound each socket operation, not the request, so an IDAM that trickles the key set
 * holds the request, and the cache lock, for as long as it keeps sending. Once {@code deadlineMs} has passed since the
 * request started, the next body read closes the stream, disconnects and throws
 * {@code IOException("IDAM JWK set retrieval exceeded its <n>ms deadline")}; Nimbus retries once, then serves the
 * cached keys.
 *
 * <p>Only {@link #openHTTPConnection} is overridden: it starts the clock and wraps the connection so
 * {@code getInputStream()} returns a checked stream. Everything else is Nimbus's own, on the JDK's
 * {@link HttpURLConnection}. A custom {@code SSLSocketFactory} is not supported; HTTPS uses the JVM default.
 *
 * <p>Bound: a read can block for up to the read timeout before the deadline is checked, so a request ends within
 * {@code max(connect time + time to headers, deadline) + read timeout}. With prompt headers that is
 * {@code max(connect timeout, deadline) + read timeout}, which {@link JwksProperties} validates the cache wait against.
 *
 * <p>Limits: the deadline only fires on body data, so slow response headers (each read bounded only by the read
 * timeout), slow DNS (inside {@code connect()}, before the connect timeout, bounded only by the OS resolver) and
 * redirect hops (each hop's own connect and headers; the JDK follows redirects by default) are not cut off. Their time
 * still counts, so headers arriving after the deadline fail at the first body read.
 *
 * <p>On close, a chunked body closes the socket; a keep-alive body with a {@code Content-Length} up to 512 KB
 * ({@code http.KeepAlive.remainingData}) is drained in the background by the JDK's keep-alive cleaner (5 s per read),
 * then reused or closed. Either way, the thread and cache lock are released at the deadline.
 */
class BodyDeadlineResourceRetriever extends DefaultResourceRetriever {

    private final int deadlineMs;

    BodyDeadlineResourceRetriever(int connectTimeoutMs, int readTimeoutMs, int sizeLimitBytes, int deadlineMs) {
        super(connectTimeoutMs, readTimeoutMs, sizeLimitBytes);
        this.deadlineMs = deadlineMs;
    }

    @Override
    protected HttpURLConnection openHTTPConnection(URL url) throws IOException {
        long startNanos = System.nanoTime();
        return new DeadlineConnection(super.openHTTPConnection(url), startNanos);
    }

    /**
     * Forwards to the JDK connection {@code connect} and {@code disconnect}, the connect and read timeouts, the request
     * method, request properties and redirect flag, the response code, message, headers, content type and error stream,
     * {@code usingProxy} and {@code getURL}, and wraps the response stream. That covers everything
     * {@code DefaultResourceRetriever#retrieveResource} uses. Other setters, such as {@code setDoOutput},
     * {@code setUseCaches} and the streaming modes, are not forwarded: they change only this wrapper, not the request.
     */
    private final class DeadlineConnection extends HttpURLConnection {

        private final HttpURLConnection delegate;
        private final long startNanos;
        private InputStream inputStream;

        DeadlineConnection(HttpURLConnection delegate, long startNanos) {
            super(delegate.getURL());
            this.delegate = delegate;
            this.startNanos = startNanos;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new DeadlineInputStream(delegate.getInputStream());
            }
            return inputStream;
        }

        @Override
        public void connect() throws IOException {
            delegate.connect();
        }

        @Override
        public void disconnect() {
            delegate.disconnect();
        }

        @Override
        public boolean usingProxy() {
            return delegate.usingProxy();
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
            delegate.setRequestMethod(method);
        }

        @Override
        public String getRequestMethod() {
            return delegate.getRequestMethod();
        }

        @Override
        public void setConnectTimeout(int timeout) {
            delegate.setConnectTimeout(timeout);
        }

        @Override
        public int getConnectTimeout() {
            return delegate.getConnectTimeout();
        }

        @Override
        public void setReadTimeout(int timeout) {
            delegate.setReadTimeout(timeout);
        }

        @Override
        public int getReadTimeout() {
            return delegate.getReadTimeout();
        }

        @Override
        public void setRequestProperty(String key, String value) {
            delegate.setRequestProperty(key, value);
        }

        @Override
        public void addRequestProperty(String key, String value) {
            delegate.addRequestProperty(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return delegate.getRequestProperty(key);
        }

        @Override
        public Map<String, List<String>> getRequestProperties() {
            return delegate.getRequestProperties();
        }

        @Override
        public void setInstanceFollowRedirects(boolean followRedirects) {
            delegate.setInstanceFollowRedirects(followRedirects);
        }

        @Override
        public boolean getInstanceFollowRedirects() {
            return delegate.getInstanceFollowRedirects();
        }

        @Override
        public int getResponseCode() throws IOException {
            return delegate.getResponseCode();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return delegate.getResponseMessage();
        }

        @Override
        public InputStream getErrorStream() {
            return delegate.getErrorStream();
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public String getHeaderField(String name) {
            return delegate.getHeaderField(name);
        }

        @Override
        public String getHeaderField(int n) {
            return delegate.getHeaderField(n);
        }

        @Override
        public String getHeaderFieldKey(int n) {
            return delegate.getHeaderFieldKey(n);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return delegate.getHeaderFields();
        }

        @Override
        public URL getURL() {
            return delegate.getURL();
        }

        /**
         * Checks the deadline after every read that returns data, and after every skip ({@code skipNBytes} goes
         * through {@code skip}). A read that reports the end of the stream is not checked: by then the whole body is
         * in hand.
         */
        private final class DeadlineInputStream extends FilterInputStream {

            DeadlineInputStream(InputStream in) {
                super(in);
            }

            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b != -1) {
                    checkDeadline();
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len);
                if (n != -1) {
                    checkDeadline();
                }
                return n;
            }

            @Override
            public long skip(long n) throws IOException {
                long skipped = super.skip(n);
                checkDeadline();
                return skipped;
            }

            /**
             * Called after a read has returned data, on the reading thread. Closing the stream here does not wait on a
             * lock held by a blocked read, as closing it from another thread would.
             */
            private void checkDeadline() throws IOException {
                if (System.nanoTime() - startNanos < TimeUnit.MILLISECONDS.toNanos(deadlineMs)) {
                    return;
                }
                try {
                    close();
                } catch (IOException e) {
                    // The request has already failed; the deadline is the error to report.
                } finally {
                    delegate.disconnect();
                }
                throw new IOException("IDAM JWK set retrieval exceeded its " + deadlineMs + "ms deadline");
            }
        }
    }
}
