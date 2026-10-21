package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs each retrieval on its own virtual thread and waits at most {@code deadlineMs} for it, so the calling thread,
 * which holds Nimbus's cache lock, is released at the deadline wherever the request is stuck: DNS, connect, response
 * headers, chunk framing, trailers or body. Connect and read timeouts bound each socket operation, not the request, so
 * without this an IDAM that trickles its response holds the lock for as long as it keeps sending.
 *
 * <p>At the deadline it interrupts the retrieval and throws
 * {@code IOException("IDAM JWK set retrieval exceeded its <n>ms deadline")}; Nimbus retries once, then serves the
 * cached keys. Interrupting a virtual thread blocked in a socket read or write closes the socket
 * ({@link java.net.Socket}), so the abandoned request ends at once, over HTTP and HTTPS. A DNS lookup does not respond
 * to the interrupt: it runs until the resolver returns, then the request fails.
 *
 * <p>At most {@value #MAX_IN_FLIGHT} retrievals run at once. When that many are still running, the next one fails at
 * once instead of queueing behind them. Errors from the retrieval are rethrown as they are. {@link #close()} stops
 * the retrievals in flight and fails every later one.
 */
class TimeLimitedResourceRetriever implements ResourceRetriever, Closeable {

    static final int MAX_IN_FLIGHT = 2;

    private final ResourceRetriever delegate;
    private final int deadlineMs;
    // A concurrency cap, not a pool: with a core size and keep-alive of 0, each retrieval gets a fresh virtual thread,
    // which ends with the retrieval.
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, MAX_IN_FLIGHT, 0, TimeUnit.SECONDS,
        new SynchronousQueue<>(), Thread.ofVirtual().name("idam-jwks-fetch-", 1).factory());

    TimeLimitedResourceRetriever(ResourceRetriever delegate, int deadlineMs) {
        this.delegate = delegate;
        this.deadlineMs = deadlineMs;
    }

    @Override
    public Resource retrieveResource(URL url) throws IOException {
        Future<Resource> retrieval;
        try {
            retrieval = executor.submit(() -> delegate.retrieveResource(url));
        } catch (RejectedExecutionException e) {
            throw new IOException(executor.isShutdown() ? "IDAM JWK set retriever is closed"
                : "IDAM JWK set retrieval not started: " + MAX_IN_FLIGHT + " earlier retrievals are still running "
                    + "past their deadline", e);
        }
        try {
            return retrieval.get(deadlineMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            retrieval.cancel(true);
            throw new IOException("IDAM JWK set retrieval exceeded its " + deadlineMs + "ms deadline");
        } catch (InterruptedException e) {
            retrieval.cancel(true);
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for the IDAM JWK set");
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case IOException cause -> throw cause;
                case RuntimeException cause -> throw cause;
                case Error cause -> throw cause;
                default -> throw new IOException(e.getCause());
            }
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
