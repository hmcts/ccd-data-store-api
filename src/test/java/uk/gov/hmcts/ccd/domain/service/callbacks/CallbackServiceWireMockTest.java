package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;

import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;
import static wiremock.com.google.common.collect.Lists.newArrayList;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
    {
        "http.client.read.timeout=500"
    })
@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class CallbackServiceWireMockTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private CallbackService callbackService;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    private CallbackResponse callbackResponse;
    private CaseDetails caseDetails = new CaseDetails();
    private final CaseEvent caseEvent = new CaseEvent();
    private String testUrl;

    @Before
    public void setUp() throws Exception {
        // IDAM
        callbackResponse = aCallbackResponse().build();
        caseDetails = newCaseDetails().build();
        final SecurityUtils securityUtils = Mockito.mock(SecurityUtils.class);
        Mockito.when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        ReflectionTestUtils.setField(callbackService, "securityUtils", securityUtils);
        testUrl = "http://localhost:" + wiremockPort + "/test-callbackGrrrr";
        WireMock.resetAllScenarios();
        WireMock.resetAllRequests();
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
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(499)));

        Instant start = Instant.now();
        callbackService.send(testUrl, null, caseEvent, null, caseDetails, CallbackResponse.class, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout + 1s retryInterval + 0.5s readTimeout + 3s retryInterval + 0.5s readTimeout
        assertThat((int) between.toMillis(), greaterThan(5500));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldRetryOnErrorWithResponseClassAndCustomRetryContext() throws Exception {

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(1001))
            .willSetStateTo("SecondFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1499)));

        List<Integer> callbackRetryTimeoutsInMillis = Lists.newArrayList(500, 1000, 1500);
        Instant start = Instant.now();
        callbackService.send(testUrl, callbackRetryTimeoutsInMillis, caseEvent, null, caseDetails, CallbackResponse.class, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout + 1s retryInterval + 1s readTimeout + 3s retryInterval + 1.5s readTimeout
        assertThat((int) between.toMillis(), greaterThan(7000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Ignore("for local dev only")
    @Test
    public void multipleCallbackRequestsWithDifferentTimeoutsDoNotClash() throws Exception {

        final List<Future<Integer>> futures = newArrayList();
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final int totalNumberOfCalls = 10;
        int[] randomDelays = new int[totalNumberOfCalls];

        setupStubs(totalNumberOfCalls, randomDelays);

        for (int i = 0; i < totalNumberOfCalls; i++) {

            Map<String, JsonNode> delay = aClassificationBuilder().withData("delay", JSON_NODE_FACTORY.textNode(String.valueOf(totalNumberOfCalls - i))).buildAsMap();

            List<Integer> callbackRetryTimeoutsInMillis = Lists.newArrayList((totalNumberOfCalls - i + 1) * 1000);

            System.out.println("delay=" + delay);
            final CaseDetails caseDetails = newCaseDetails().withDataClassification(delay).build();
            futures.add(executorService.submit(() -> {
                final ResponseEntity<CallbackResponse> response =
                    callbackService.send(testUrl, callbackRetryTimeoutsInMillis, caseEvent, null, caseDetails, CallbackResponse.class, false);
                return response.getStatusCode().value();
            }));
        }

        assertThat(futures, hasSize(totalNumberOfCalls));

        for (Future<Integer> future : futures) {
            assertThat(future.get(), is(SC_OK));
        }

    }

    private void setupStubs(final int totalNumberOfCalls, final int[] randomDelays) throws NoSuchAlgorithmException, JsonProcessingException {
        for (int i = 0; i < totalNumberOfCalls; i++) {
            System.out.println("delay=" + (totalNumberOfCalls - i));

            Map<String, JsonNode> delay = aClassificationBuilder().withData("delay", JSON_NODE_FACTORY.textNode(String.valueOf(totalNumberOfCalls - i))).buildAsMap();
            CallbackResponse callbackResponse = aCallbackResponse().withDataClassification(delay).build();

            if (i == 0) {
                System.out.println("i=" + i);
                System.out.println("fixedDelay=" + (totalNumberOfCalls - i) * 1000);
                stubFor(post(urlMatching("/test-callbackGrrrr.*"))
                    .inScenario("CallbackSequence")
                    .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay((totalNumberOfCalls - i) * 1000))
                    .willSetStateTo(String.valueOf(i)));
            } else {
                System.out.println("i=" + i);
                System.out.println("fixedDelay=" + (totalNumberOfCalls - i) * 1000);
                stubFor(post(urlMatching("/test-callbackGrrrr.*"))
                    .inScenario("CallbackSequence")
                    .whenScenarioStateIs(String.valueOf(i - 1))
                    .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay((totalNumberOfCalls - i) * 1000))
                    .willSetStateTo(String.valueOf(i)));
            }
        }
    }
}
