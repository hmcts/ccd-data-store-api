package uk.gov.hmcts.ccd;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.io.IOException;

@DirtiesContext  // required for Jenkins agent
@AutoConfigureWireMock(port = 0)
public abstract class WireMockBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    @Inject
    private ApplicationParams applicationParams;

    @Before
    public void initMock() throws IOException {
        super.initMock();
        final String hostUrl = "http://localhost:" + wiremockPort;

        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "idamHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
    }
}
