package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.io.IOException;

@AutoConfigureWireMock(port = 0)
public abstract class WireMockBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    public static final int NUMBER_OF_CASES = 19;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    @Inject
    protected ApplicationParams applicationParams;

    @Inject
    protected WireMockServer wireMockServer;

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        super.initMock();
        final String hostUrl = "http://localhost:" + wiremockPort;

        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
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
}
