package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@DirtiesContext  // required for Jenkins agent
public abstract class WireMockBaseTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    @ClassRule  // use next available port
    public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig().port(0).notifier(slf4jNotifier)
            .disableRequestJournal());
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Inject
    private ApplicationParams applicationParams;

    @Before
    public void initMock() throws IOException {
        super.initMock();
        final Integer port = instanceRule.port();
        final String hostUrl = "http://localhost:" + port;
        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "idamHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
    }
}
