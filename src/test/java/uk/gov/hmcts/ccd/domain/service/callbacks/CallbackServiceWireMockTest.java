package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.service.callbacks.retrycontext.CallbackRetryContextBuilder;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;
import static wiremock.com.google.common.collect.Lists.newArrayList;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
    {
        "ccd.callback.timeouts=1,2,3"
    })
@DirtiesContext
public class CallbackServiceWireMockTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private CallbackService callbackService;

    private CallbackResponse callbackResponse;
    private CaseDetails caseDetails = new CaseDetails();
    private final CaseEvent caseEvent = new CaseEvent();
    private static String testUrl;

    @Rule
    private WireMockServer ws = new WireMockServer(WireMockConfiguration.options()
        // Set the number of request handling threads in Jetty. Defaults to 10.
        .containerThreads(100)
        // Set the number of connection acceptor threads in Jetty. Defaults to 2.
        .jettyAcceptors(80)
        // Set the Jetty accept queue size. Defaults to Jetty's default of unbounded.
        .jettyAcceptQueueSize(200)
        .dynamicPort());

    @BeforeEach
    void setUpEach() {
        callbackResponse = aCallbackResponse().build();
        caseDetails = newCaseDetails().build();
        final SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        ReflectionTestUtils.setField(callbackService, "securityUtils", securityUtils);

        ws.start();
        testUrl = "http://localhost:" + ws.port() + "/test-callbackGrrrr";
        ws.resetAll();
    }


    @Test
    public void happyPathWithNoErrorsOrWarnings() throws Exception {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        ws.stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, null, caseDetails);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertTrue(response.getErrors().isEmpty());
    }

    @Disabled("investigating socket issues in Azure")
    @Test
    public void shouldRetryIfCallbackRespondsLate() throws Exception {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        ws.stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1500)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, null, caseDetails);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));
        ws.verify(2, newRequestPattern(RequestMethod.POST, urlMatching("/test-callback.*")));
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void failurePathWithErrors() throws Exception {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Test message"));
        callbackResponse.setData(caseDetails.getData());
        ws.stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, null, caseDetails);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), Matchers.contains("Test message"));
    }

    @Test
    public void notFoundFailurePath() throws Exception {
        final String testUrl = "http://localhost";
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, null, caseDetails));
    }

    @Test
    public void serverError() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        ws.stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, null, caseDetails));
    }

    @Test
    public void retryOnServerError() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, null, caseEvent, null, caseDetails);
        } catch (Exception e) {
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        ws.verify(3, newRequestPattern(POST, urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void authorisationError() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        ws.stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(401)));

        assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, null, caseDetails));
    }

    @Test
    public void validateCallbackErrorsAndWarningsHappyPath() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithWarnings() {
        final String TEST_WARNING_1 = "WARNING 1";
        final String TEST_WARNING_2 = "WARNING 2";

        final CallbackResponse callbackResponse = new CallbackResponse();
        final List<String> warnings = new ArrayList<>();
        warnings.add(TEST_WARNING_1);
        warnings.add(TEST_WARNING_2);

        callbackResponse.setWarnings(warnings);
        assertThrows(ApiException.class, () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false));
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithErrorsAndIgnore() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));
        callbackResponse.setWarnings(Collections.singletonList("a warning"));
        assertThrows(ApiException.class, () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true));
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithWarningsAndIgnore() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);

        final List<String> warnings = Collections.singletonList("Test");
        callbackResponse.setWarnings(warnings);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
    }

    @Test
    public void shouldGetBodyInGeneric() throws Exception {
        final String testUrl = "http://localhost:" + ws.port() + "/test-callback-submitted";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        ws.stubFor(post(urlMatching("/test-callback-submitted.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(201)));

        final ResponseEntity<String> result = callbackService.send(testUrl, null, caseEvent, null, caseDetails,
            String.class);

        assertAll(
            () -> assertThat(result.getStatusCodeValue(), is(201)),
            () -> JSONAssert.assertEquals(
                "{\"data\":null,\"errors\":[],\"warnings\":[],\"data_classification\":null,\"security_classification\":null}",
                result.getBody(),
                JSONCompareMode.LENIENT)
        );
    }

    @Test
    public void shouldRetryOnError() throws Exception {
        String url = "http://localhost:" + ws.port() + "/test-callback-invaliddd";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        ws.stubFor(post(urlMatching("/test-callback-invaliddd.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(url, null, caseEvent, null, caseDetails, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        ws.verify(3, newRequestPattern(POST, urlMatching("/test-callback-invaliddd.*")));
    }

    @Test
    public void shouldThrowCallbackException_whenSendInvalidUrlGetGenericBody() throws Exception {
        final String testUrl = "http://localhost/invalid-test-callback";
        final RestTemplate restTemplate = mock(RestTemplate.class);
        final ApplicationParams applicationParams = mock(ApplicationParams.class);
        given(applicationParams.getCallbackRetryIntervalsInSeconds()).willReturn(Arrays.asList(3, 5));

        // Builds a new callback service to avoid wiremock exception to get in the way
        final CallbackService underTest = new CallbackService(mock(SecurityUtils.class), restTemplate, mock(ExecutorService.class), mock(CallbackRetryContextBuilder.class));
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        CallbackException callbackException = assertThrows(CallbackException.class, () -> underTest.send(testUrl, null, caseEvent, null, caseDetails, String.class));
        assertThat(callbackException.getMessage(), is("Unsuccessful callback to url=http://localhost/invalid-test-callback"));
    }

    @Test
    public void shouldRetryOnErrorWithIgnoreWarningFalseAndDefaultRetryContext() throws Exception {

        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("SecondFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(490)));

        Instant start = Instant.now();
        callbackService.send(testUrl, null, caseEvent, null, caseDetails, CallbackResponse.class, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 1s readTimeout + 1s retryInterval + 0.5s readTimeout + 3s retryInterval + 0.49s readTimeout
        assertThat((int) between.toMillis(), greaterThan(5500));
        ws.verify(3, newRequestPattern(RequestMethod.POST, urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldNotRetryWhenCallbackRetriesDisabled() throws Exception {

        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));

        List<Integer> disabledRetries = Lists.newArrayList(0);
        Instant start = Instant.now();

        CallbackException callbackException = assertThrows(CallbackException.class, () -> callbackService.send(testUrl, disabledRetries, caseEvent, null, caseDetails, CallbackResponse.class, false));
        assertThat(callbackException.getMessage(), is("Unsuccessful callback to url=http://localhost:" + ws.port() + "/test-callbackGrrrr"));
        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout and no follow up retries
        assertThat((int) between.toMillis(), lessThan(1500));
        ws.verify(1, newRequestPattern(RequestMethod.POST, urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldRetryOnServerErrorWithCustomRetryContext() throws Exception {

        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(1001))
            .willSetStateTo("SecondFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1499)));

        List<Integer> callbackRetryTimeoutsInSeconds = Lists.newArrayList(1, 2, 3);
        Instant start = Instant.now();
        callbackService.send(testUrl, callbackRetryTimeoutsInSeconds, caseEvent, null, caseDetails, CallbackResponse.class, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 0.5s readTimeout + 1s retryInterval + 1s readTimeout + 3s retryInterval + 1.5s readTimeout = 7s
        assertThat((int) between.toMillis(), greaterThan(7000));
        ws.verify(3, newRequestPattern(POST, urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldRetryOnTimeoutErrorWithCustomRetryContext() throws Exception {

        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1001))
            .willSetStateTo("FirstFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(2001))
            .willSetStateTo("SecondFailedAttempt"));
        ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1499)));

        List<Integer> callbackRetryTimeoutsInSeconds = Lists.newArrayList(1, 2, 3);
        Instant start = Instant.now();
        callbackService.send(testUrl, callbackRetryTimeoutsInSeconds, caseEvent, null, caseDetails, CallbackResponse.class, false);

        final Duration between = Duration.between(start, Instant.now());
        // 0s retryInterval + 1s readTimeout + 1s retryInterval + 2s readTimeout + 3s retryInterval + 1.5s readTimeout = 8.5
        assertThat((int) between.toMillis(), greaterThan(8500));
        ws.verify(3, newRequestPattern(POST, urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void multipleCallbackRequestsWithDifferentTimeoutsDoNotClash() throws Exception {

        final List<Future<Integer>> futures = newArrayList();
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final int totalNumberOfCalls = 100;

        setupStubs(totalNumberOfCalls);

        for (int i = 0; i < totalNumberOfCalls; i++) {

            Map<String, JsonNode> delay = aClassificationBuilder().withData("delay", JSON_NODE_FACTORY.textNode(String.valueOf(totalNumberOfCalls - i))).buildAsMap();

            List<Integer> callbackRetryTimeoutsInSeconds = Lists.newArrayList(10);

            System.out.println("delay=" + delay);
            final CaseDetails caseDetails = newCaseDetails().withDataClassification(delay).build();
            futures.add(executorService.submit(() -> {
                final ResponseEntity<CallbackResponse> response =
                    callbackService.send(testUrl, callbackRetryTimeoutsInSeconds, caseEvent, null, caseDetails, CallbackResponse.class, false);
                return response.getStatusCode().value();
            }));
        }

        assertThat(futures, hasSize(totalNumberOfCalls));

        for (Future<Integer> future : futures) {
            assertThat(future.get(), is(SC_OK));
        }

    }

    private void setupStubs(final int totalNumberOfCalls) throws JsonProcessingException {
        for (int i = 0; i < totalNumberOfCalls; i++) {
            System.out.println("delay=" + (totalNumberOfCalls - i));

            Map<String, JsonNode> delay = aClassificationBuilder().withData("delay", JSON_NODE_FACTORY.textNode(String.valueOf(totalNumberOfCalls - i))).buildAsMap();
            CallbackResponse callbackResponse = aCallbackResponse().withDataClassification(delay).build();
            int fixedDelay = ((totalNumberOfCalls - i) % 5) * 1000;

            if (i == 0) {
                System.out.println("i=" + i);
                System.out.println("fixedDelay=" + fixedDelay);
                ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
                    .inScenario("CallbackSequence")
                    .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(fixedDelay))
                    .willSetStateTo(String.valueOf(i)));
            } else {
                System.out.println("i=" + i);
                System.out.println("fixedDelay=" + fixedDelay);
                ws.stubFor(post(urlMatching("/test-callbackGrrrr.*"))
                    .inScenario("CallbackSequence")
                    .whenScenarioStateIs(String.valueOf(i - 1))
                    .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(fixedDelay))
                    .willSetStateTo(String.valueOf(i)));
            }
        }
    }
}
