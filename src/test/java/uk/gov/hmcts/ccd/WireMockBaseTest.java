package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

@AutoConfigureWireMock(port = 0)
public abstract class WireMockBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    public static final int NUMBER_OF_CASES = 20;

    private static final String BEARER = "Bearer ";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODI2MDAyMzN9"
        + ".Lz467pTdzRF0MGQye8QDzoLLY_cxk79ZB3OOYdOR-0PGYK5sVay4lxOvhIa-1VnfizaaDDZUwmPdMwQOUBfpBQ";
    private static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    protected String hostUrl;

    @Inject
    protected ApplicationParams applicationParams;

    @Inject
    protected WireMockServer wireMockServer;

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        super.initMock();
        hostUrl = "http://localhost:" + wiremockPort;

        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", hostUrl);
    }

    public void stubFor(MappingBuilder mappingBuilder) {
        wireMockServer.stubFor(mappingBuilder);
    }

    public void verifyWireMock(int count, RequestPatternBuilder postRequestedFor) {
        wireMockServer.verify(count, postRequestedFor);
    }

    public void stubSuccess(final String path, final String payload, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody(payload)
            )
        );
    }

    public void stubUpstreamFault(final String path, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .willReturn(aResponse()
                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    }

    public void editSuccessStub(final String path, final String payload, final UUID mappingId) {
        wireMockServer.editStub(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody(payload)
            )
        );
    }

    public void stubNotFound(final String path, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
            )
        );
    }

    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        return objectMapper;
    }

    public String objectToJsonString(final Object object) {
        try {
            final ObjectMapper objectMapper = objectMapper();

            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Configuration
    static class WireMockTestConfiguration {

        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
            return config -> config.extensions(new ResponseDefinitionTransformer() {

                @Override
                public String getName() {
                    return "keep-alive-disabler";
                }

                @Override
                public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                                    FileSource files, Parameters parameters) {
                    return ResponseDefinitionBuilder.like(responseDefinition)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .build();
                }
            });
        }
    }

    protected void stubUserInfo(String userId) {
        stubUserInfo(userId, "caseworker", "caseworker-test", "caseworker-probate-public", "caseworker-probate",
            "caseworker-divorce", "caseworker-sscs");
    }

    protected void stubUserInfo(String userId, String... roles) {
        String rolesList = Stream.of(roles)
            .map(e -> "\"" + e + "\"")
            .collect(Collectors.joining(","));
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson("{"
                + "      \"uid\": \"" + userId + "\","
                + "      \"sub\": \"Cloud.Strife@test.com\","
                + "      \"roles\": [ " + rolesList + " ]"
                + "    }").withStatus(200)));
    }

    protected void stubIdamRolesForUser(String userId) {
        stubFor(WireMock.get(urlMatching("/api/v1/users/" + userId))
            .willReturn(okJson("{"
                + "      \"id\": \" " + userId + "\","
                + "      \"email\": \"Cloud.Strife@test.com\","
                + "      \"forename\": \"Cloud\","
                + "      \"surname\": \"Strife\","
                + "      \"roles\": [ \"caseworker\", \"caseworker-test\", \"caseworker-probate-public\","
                + " \"caseworker-probate\", \"caseworker-divorce\", \"caseworker-sscs\" ]"
                + "    }").withStatus(200)));
    }
}
