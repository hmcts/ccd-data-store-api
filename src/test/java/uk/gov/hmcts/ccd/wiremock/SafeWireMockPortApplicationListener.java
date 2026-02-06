package uk.gov.hmcts.ccd.wiremock;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Properties;

/**
 * Spring Cloud Contract's {@code WireMockApplicationListener} picks "dynamic" ports using a
 * find-free-then-bind approach, which is prone to collisions when tests run in parallel forks.
 *
 * <p>This listener runs first and replaces the dynamic port (0) with a reserved port (non-zero),
 * using a cross-process lock + reservation file to avoid multiple forks selecting the same port.
 */
public final class SafeWireMockPortApplicationListener
    implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

    private static final String WIREMOCK_PROPERTY_SOURCE = "wiremock";
    private static final String WIREMOCK_SERVER_PORT = "wiremock.server.port";
    private static final String WIREMOCK_SERVER_PORT_DYNAMIC = "wiremock.server.port-dynamic";
    private static final String WIREMOCK_HTTPS_PORT = "wiremock.server.https-port";

    // Match the default range used by Spring Cloud Contract's WireMockApplicationListener.
    private static final int PORT_RANGE_MIN = 10_000;
    private static final int PORT_RANGE_MAX = 12_500;

    private static final long RESERVATION_TTL_MS = 2 * 60 * 1000L;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();

        Integer port = env.getProperty(WIREMOCK_SERVER_PORT, Integer.class);
        if (port == null || port.intValue() != 0) {
            // No WireMock dynamic port requested; nothing to do.
            return;
        }

        int reservedPort = reserveHttpPort();
        putWireMockProperty(env, WIREMOCK_SERVER_PORT, reservedPort);
        putWireMockProperty(env, WIREMOCK_SERVER_PORT_DYNAMIC, true);

        // If https-port isn't explicitly set to -1, Spring Cloud Contract will attempt to allocate a
        // dynamic https port too (and then start WireMock with HTTPS enabled). Keep tests HTTP-only.
        Integer httpsPort = env.getProperty(WIREMOCK_HTTPS_PORT, Integer.class);
        if (httpsPort == null) {
            putWireMockProperty(env, WIREMOCK_HTTPS_PORT, -1);
        }
    }

    private static void putWireMockProperty(ConfigurableEnvironment env, String key, Object value) {
        MutablePropertySources propertySources = env.getPropertySources();

        MapPropertySource wiremockSource;
        if (propertySources.contains(WIREMOCK_PROPERTY_SOURCE)) {
            wiremockSource = (MapPropertySource) propertySources.get(WIREMOCK_PROPERTY_SOURCE);
            if (propertySources.get(WIREMOCK_PROPERTY_SOURCE) != propertySources.iterator().next()) {
                propertySources.remove(WIREMOCK_PROPERTY_SOURCE);
                propertySources.addFirst(wiremockSource);
            }
        } else {
            wiremockSource = new MapPropertySource(WIREMOCK_PROPERTY_SOURCE, new HashMap<>());
            propertySources.addFirst(wiremockSource);
        }

        wiremockSource.getSource().put(key, value);
    }

    private static int reserveHttpPort() {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path lockPath = tmpDir.resolve("ccd-wiremock-port.lock");
        Path reservationsPath = tmpDir.resolve("ccd-wiremock-port-reservations.properties");

        try {
            Files.createDirectories(tmpDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp directory for WireMock port allocation", e);
        }

        try (FileChannel channel = FileChannel.open(lockPath,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            try (FileLock ignored = channel.lock()) {
                Properties reservations = loadReservations(reservationsPath);
                pruneStaleReservations(reservations);

                int reserved = findAndReservePort(reservations);
                storeReservations(reservationsPath, reservations);
                return reserved;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to allocate a WireMock port under lock", e);
        }
    }

    private static Properties loadReservations(Path reservationsPath) throws IOException {
        Properties props = new Properties();
        if (!Files.exists(reservationsPath)) {
            return props;
        }

        try (InputStream in = Files.newInputStream(reservationsPath, StandardOpenOption.READ)) {
            props.load(in);
        }
        return props;
    }

    private static void storeReservations(Path reservationsPath, Properties reservations) throws IOException {
        try (OutputStream out = Files.newOutputStream(reservationsPath,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            reservations.store(out, "Reserved WireMock HTTP ports for parallel test forks");
        }
    }

    private static void pruneStaleReservations(Properties reservations) {
        long now = System.currentTimeMillis();
        reservations.entrySet().removeIf(e -> {
            try {
                long ts = Long.parseLong(e.getValue().toString());
                return now - ts > RESERVATION_TTL_MS;
            } catch (NumberFormatException ex) {
                return true;
            }
        });
    }

    private static int findAndReservePort(Properties reservations) {
        long now = System.currentTimeMillis();

        for (int p = PORT_RANGE_MIN; p <= PORT_RANGE_MAX; p++) {
            String key = String.valueOf(p);
            if (reservations.containsKey(key)) {
                continue;
            }
            if (!isLocalPortAvailable(p)) {
                continue;
            }
            reservations.setProperty(key, String.valueOf(now));
            return p;
        }

        throw new IllegalStateException("No free WireMock port available in range "
            + PORT_RANGE_MIN + "-" + PORT_RANGE_MAX);
    }

    private static boolean isLocalPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))) {
            socket.setReuseAddress(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
