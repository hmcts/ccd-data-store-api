package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NativeErrorResponseTest.Probe.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.config.location=optional:classpath:review-empty.properties",
        "server.error.include-message=never", "server.error.include-stacktrace=never", "logging.level.root=INFO"})
class NativeErrorResponseTest {
    @LocalServerPort
    int port;

    @Configuration(proxyBeanMethods = false)
    @Import(ErrorPageConfiguration.class)
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class})
    static class Probe {
    }

    @Test
    void malformedMethodDoesNotDiscloseNativeDiagnostics() throws Exception {
        try (var socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write("G[ET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                .getBytes(StandardCharsets.US_ASCII));
            var response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(response).startsWith("HTTP/1.1 400");
            assertThat(response).doesNotContain("java.lang", "org.apache", "Apache Tomcat", "Exception", "G[ET");
        }
    }
}
