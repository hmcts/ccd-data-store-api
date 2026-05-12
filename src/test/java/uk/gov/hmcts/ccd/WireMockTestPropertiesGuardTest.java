package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WireMockTestPropertiesGuardTest {

    @Test
    void shouldNotUseFixedWireMockFallbackPortsInTestProperties() throws IOException {
        InputStream input = Objects.requireNonNull(
            getClass().getResourceAsStream("/test.properties"),
            "test.properties was not found on test classpath"
        );
        String testProperties = new String(input.readAllBytes(), StandardCharsets.UTF_8);

        assertFalse(testProperties.contains("${wiremock.server.port:5000}"),
            "Fixed WireMock fallback port 5000 must not be used in test.properties");
        assertFalse(testProperties.contains("${wiremock.server.port:4502}"),
            "Fixed WireMock fallback port 4502 must not be used in test.properties");
    }
}
