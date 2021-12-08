package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

@AutoConfigureWireMock(port = 0)
public abstract class WireMockBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    public static final int NUMBER_OF_CASES = 19;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    protected String hostUrl;

    @Mock
    protected CaseDefinitionRepository caseDefinitionRepository;

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
