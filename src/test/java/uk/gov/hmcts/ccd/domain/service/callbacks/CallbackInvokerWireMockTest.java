package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;

@TestPropertySource(properties =
    {
        "http.client.read.timeout=500"
    })
public class CallbackInvokerWireMockTest extends WireMockBaseTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper mapper = JacksonUtils.MAPPER;

    @Inject
    private CallbackInvoker callbackInvoker;

    private CallbackResponse callbackResponse;
    private CaseDetails caseDetails = new CaseDetails();
    private final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
    private String testUrl;

    @Before
    public void setUp() throws Exception {
        // IDAM
        callbackResponse = aCallbackResponse().build();
        caseDetails = newCaseDetails().build();

        testUrl = "http://localhost:" + wiremockPort + "/test-callbackGrrrr";
        caseEventDefinition.setCallBackURLAboutToStartEvent(testUrl);
        caseEventDefinition.setName("Test");
    }

    // @Test FIXME: flakey one need some investigation - RDM-7504
    public void shouldRetryOnErrorWithIgnoreWarningFalseAndDefaultRetryContext() throws Exception {

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("SecondFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(490)));

        Instant start = Instant.now();
        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout + 1s retryInterval + 0.5s readTimeout + 3s retryInterval + 0.49s
        // readTimeout
        assertThat((int) between.toMillis(), greaterThan(5500));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldNotRetryWhenCallbackRetriesDisabled() throws Exception {

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));

        List<Integer> disabledRetries = Lists.newArrayList(0);
        caseEventDefinition.setRetriesTimeoutAboutToStartEvent(disabledRetries);
        Instant start = Instant.now();

        CallbackException callbackException = assertThrows(CallbackException.class, () ->
            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false));
        Assert.assertThat(callbackException.getMessage(), is("Callback to service has been unsuccessful for "
                + "event Test"));
        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout and no follow up retries
        assertThat((int) between.toMillis(), lessThan(1500));
        verify(exactly(1), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

}
