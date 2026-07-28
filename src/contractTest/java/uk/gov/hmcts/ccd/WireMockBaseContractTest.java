package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import org.wiremock.spring.WireMockConfigurationCustomizer;

import jakarta.inject.Inject;
import java.io.IOException;

@EnableWireMock(@ConfigureWireMock(configurationCustomizers = WireMockBaseContractTest.Customizer.class))
public abstract class WireMockBaseContractTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseContractTest.class);

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    public String hostUrl;

    @Inject
    protected ApplicationParams applicationParams;

    @InjectWireMock
    protected WireMockServer wireMockServer;

    @BeforeEach
    public void initMock() throws IOException {
        hostUrl = "http://localhost:" + wiremockPort;

        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
    }

    protected void stubFor(MappingBuilder mappingBuilder) {
        wireMockServer.stubFor(mappingBuilder);
    }

    public static class Customizer implements WireMockConfigurationCustomizer {

        @Override
        public void customize(WireMockConfiguration configuration, ConfigureWireMock options) {
            configuration.extensions(new ResponseDefinitionTransformer() {

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
