package uk.gov.hmcts.ccd.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that Spring Boot's OAuth2 client auto-configuration, fed the production
 * {@code src/main/resources/application.properties}, builds the {@code oidc} client registration without any
 * request to IDAM. With {@code provider.oidc.issuer-uri} set, Boot fetches IDAM's discovery document while the
 * context starts, so the application could not start while IDAM was unreachable (CCD-8077).
 *
 * <p>{@code TestIdamConfiguration} replaces the {@link ClientRegistrationRepository} in every Spring test, so only a
 * test like this one exercises Boot's own repository. The properties are read from the source file, not copied, so
 * that reintroducing an issuer-uri there fails this test.
 */
class ClientRegistrationStartupTest {

    private static final Path MAIN_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final String PROVIDER = "spring.security.oauth2.client.provider.oidc.";
    private static final String REGISTRATION = "spring.security.oauth2.client.registration.oidc.";
    private static final String DISCOVERY_PATHS = "/o/\\.well-known/.*";

    private WireMockServer idam;

    @BeforeEach
    void startIdam() {
        idam = JwkTestSupport.startWireMock();
    }

    @AfterEach
    void stopIdam() {
        idam.stop();
    }

    @Test
    @DisplayName("builds the oidc client registration from the production properties without contacting IDAM")
    void buildsTheRegistrationWithoutContactingIdam() {
        contextRunner(mainProperties()).run(context -> {
            assertThat(context).hasNotFailed();
            ClientRegistration registration =
                context.getBean(ClientRegistrationRepository.class).findByRegistrationId("oidc");

            assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo(idamUrl() + "/o/token");
            assertThat(registration.getAuthorizationGrantType())
                .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
            assertThat(idam.findAll(getRequestedFor(urlPathMatching(DISCOVERY_PATHS))))
                .as("no OIDC discovery request")
                .isEmpty();
            assertThat(idam.findAll(anyRequestedFor(urlPathMatching(".*"))))
                .as("no request to IDAM at all")
                .isEmpty();
        });
    }

    @Test
    @DisplayName("negative control: the previous issuer-uri configuration does fetch the discovery document")
    void previousIssuerUriConfigurationPerformsDiscovery() {
        stubDiscovery();
        Map<String, Object> previous = new HashMap<>(asMap(mainProperties()));
        previous.remove(PROVIDER + "token-uri");
        previous.remove(REGISTRATION + "authorization-grant-type");
        previous.put(PROVIDER + "issuer-uri", "${IDAM_OIDC_URL:http://localhost:5000}/o");

        contextRunner(new MapPropertySource("previous application.properties", previous)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(idam.findAll(getRequestedFor(urlPathMatching(DISCOVERY_PATHS))))
                .as("Boot fetched the discovery document while the context started")
                .hasSize(1);
        });
    }

    @Test
    @DisplayName("negative control: an issuer-uri added alongside token-uri still triggers discovery")
    void issuerUriAddedToTheProductionPropertiesPerformsDiscovery() {
        stubDiscovery();

        contextRunner(mainProperties())
            .withPropertyValues(PROVIDER + "issuer-uri=" + idamUrl() + "/o")
            .run(context -> assertThat(idam.findAll(getRequestedFor(urlPathMatching(DISCOVERY_PATHS))))
                .hasSize(1));
    }

    private ApplicationContextRunner contextRunner(PropertySource<?> applicationProperties) {
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
            .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(applicationProperties))
            .withPropertyValues("IDAM_OIDC_URL=" + idamUrl());
    }

    private String idamUrl() {
        return "http://localhost:" + idam.port();
    }

    private void stubDiscovery() {
        String issuer = idamUrl() + "/o";
        idam.stubFor(get(urlEqualTo("/o/.well-known/openid-configuration")).willReturn(okJson("{"
            + "\"issuer\":\"" + issuer + "\","
            + "\"authorization_endpoint\":\"" + issuer + "/authorize\","
            + "\"token_endpoint\":\"" + issuer + "/token\","
            + "\"jwks_uri\":\"" + issuer + "/jwks\","
            + "\"response_types_supported\":[\"code\"],"
            + "\"subject_types_supported\":[\"public\"],"
            + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}")));
    }

    private static PropertySource<?> mainProperties() {
        assertThat(MAIN_PROPERTIES).as("run from the project directory").isRegularFile();
        try {
            List<PropertySource<?>> sources = new PropertiesPropertySourceLoader()
                .load("main application.properties", new FileSystemResource(MAIN_PROPERTIES));
            assertThat(sources).hasSize(1);
            assertThat(Files.readString(MAIN_PROPERTIES)).contains(REGISTRATION + "client-id");
            return sources.getFirst();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + MAIN_PROPERTIES, e);
        }
    }

    private static Map<String, Object> asMap(PropertySource<?> source) {
        EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
        Map<String, Object> values = new HashMap<>();
        for (String name : enumerable.getPropertyNames()) {
            values.put(name, String.valueOf(enumerable.getProperty(name)));
        }
        return values;
    }
}
