package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@TestPropertySource(properties =
    {
    "http.client.read.timeout=500",
    "http.client.connection.timeout=200"
    })
public class CallbackInvokerWireMockTest extends WireMockBaseTest {

    private static final ObjectMapper mapper = JacksonUtils.MAPPER;

    @Inject
    private CallbackInvoker callbackInvoker;

    private CallbackResponse callbackResponse;
    private CaseDetails caseDetails = new CaseDetails();
    private final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

    @BeforeEach
    public void setUp() throws Exception {
        // IDAM
        callbackResponse = aCallbackResponse().build();
        caseDetails = newCaseDetails().build();

        String testUrl = hostUrl + "/test-callbackGrrrr";
        caseEventDefinition.setCallBackURLAboutToStartEvent(testUrl);
        caseEventDefinition.setName("Test");
        wireMockServer.resetAll();
    }

    @Test
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
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200))
            .willSetStateTo("SuccessfulAttempt"));

        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false);

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

        CallbackException callbackException = assertThrows(CallbackException.class, () ->
            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false));
        MatcherAssert.assertThat(callbackException.getMessage(),
            CoreMatchers.containsString("Callback to service has been unsuccessful for event Test url"));
        verify(exactly(1), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void aboutToSubmitShouldRespectReadTimeout() throws Exception {
        String submitUrl = hostUrl + "/about-to-submit-timeout";
        caseEventDefinition.setCallBackURLAboutToSubmitEvent(submitUrl);
        caseEventDefinition.setRetriesTimeoutURLAboutToSubmitEvent(Lists.newArrayList(0));
        stubFor(post(urlMatching("/about-to-submit-timeout.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(700)));

        Instant start = Instant.now();
        CallbackException ex = assertThrows(CallbackException.class, () ->
            callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition, caseDetails, caseDetails, caseTypeDefinition, false));
        Duration duration = Duration.between(start, Instant.now());

        MatcherAssert.assertThat("should fail fast on read timeout", duration.toMillis() < 2_000L);
        MatcherAssert.assertThat("exception should mention unsuccessful callback",
            ex.getMessage(), CoreMatchers.containsString("Callback to service has been unsuccessful"));
        verify(exactly(1), postRequestedFor(urlMatching("/about-to-submit-timeout.*")));
    }

    @Test
    public void aboutToSubmitShouldRejectNonAllowlistedHostFast() {
        String unreachableUrl = "http://10.255.255.1:9/unreachable-callback";
        caseEventDefinition.setCallBackURLAboutToSubmitEvent(unreachableUrl);
        caseEventDefinition.setRetriesTimeoutURLAboutToSubmitEvent(Lists.newArrayList(0));

        Instant start = Instant.now();
        CallbackException ex = assertThrows(CallbackException.class, () ->
            callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition, caseDetails, caseDetails, caseTypeDefinition, false));
        Duration duration = Duration.between(start, Instant.now());

        MatcherAssert.assertThat("non-allowlisted host should fail fast",
            duration.toMillis() < 2_000L);
        MatcherAssert.assertThat("exception should mention callback host validation",
            ex.getMessage(), CoreMatchers.containsString("host is not allowlisted"));
    }

}
