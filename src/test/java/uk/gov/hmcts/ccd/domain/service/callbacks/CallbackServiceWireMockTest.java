package uk.gov.hmcts.ccd.domain.service.callbacks;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
    {
        "ccd.callback.timeouts=1,2,3"
    })
@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class CallbackServiceWireMockTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private CallbackService callbackService;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    @Before
    public void setUp() {
        // IDAM
        final SecurityUtils securityUtils = Mockito.mock(SecurityUtils.class);
        Mockito.when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        ReflectionTestUtils.setField(callbackService, "securityUtils", securityUtils);
    }

    @Test
    public void happyPathWithNoErrorsOrWarnings() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, caseEvent, null, caseDetails, false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertTrue(response.getErrors().isEmpty());
    }

    @org.junit.Ignore // TODO investigating socket issues in Azure
    @Test
    public void shouldRetryIfCallbackRespondsLate() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1500)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, caseEvent, null, caseDetails, false);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));
        verify(exactly(2), postRequestedFor(urlMatching("/test-callback.*")));
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void failurePathWithErrors() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Test message"));
        callbackResponse.setData(caseDetails.getData());
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, caseEvent, null, caseDetails, false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), Matchers.contains("Test message"));
    }

    @Test(expected = CallbackException.class)
    public void notFoundFailurePath() throws Exception {
        final String testUrl = "http://localhost";
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        callbackService.send(testUrl, caseEvent, null, caseDetails, false);
    }

    @Test(expected = CallbackException.class)
    public void serverError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        callbackService.send(testUrl, caseEvent, null, caseDetails, false);
    }

    @Test
    public void retryOnServerError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callbackGrrrr";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, caseEvent, null, caseDetails, false);
        } catch (Exception e) {
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test(expected = CallbackException.class)
    public void authorisationError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(401)));

        callbackService.send(testUrl, caseEvent, null, caseDetails, false);
    }

    @Test
    public void validateCallbackErrorsAndWarningsHappyPath() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithWarnings() throws Exception {
        final String TEST_WARNING_1 = "WARNING 1";
        final String TEST_WARNING_2 = "WARNING 2";

        final CallbackResponse callbackResponse = new CallbackResponse();
        final List<String> warnings = new ArrayList<>();
        warnings.add(TEST_WARNING_1);
        warnings.add(TEST_WARNING_2);

        callbackResponse.setWarnings(warnings);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithErrorsAndIgnore() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));
        callbackResponse.setWarnings(Collections.singletonList("a warning"));
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithWarningsAndIgnore() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);

        final List<String> warnings = Collections.singletonList("Test");
        callbackResponse.setWarnings(warnings);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
    }

    @Test
    public void shouldGetBodyInGeneric() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback-submitted";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-submitted.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(201)));

        final ResponseEntity<String> result = callbackService.send(testUrl, caseEvent, null, caseDetails,
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
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback-invaliddd";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-invaliddd.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, caseEvent, null, caseDetails, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callback-invaliddd.*")));
    }

    @Test(expected = CallbackException.class)
    public void shouldThrowCallbackException_whenSendInvalidUrlGetGenericBody() throws Exception {
        final String testUrl = "http://localhost/invalid-test-callback";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ApplicationParams applicationParams = Mockito.mock(ApplicationParams.class);
        given(applicationParams.getCallbackRetries()).willReturn(Arrays.asList(3, 5));

        // Builds a new callback service to avoid wiremock exception to get in the way
        final CallbackService underTest = new CallbackService(Mockito.mock(SecurityUtils.class), restTemplate);
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEvent caseEvent = new CaseEvent();
        caseEvent.setId("TEST-EVENT");

        try {
            underTest.send(testUrl, caseEvent, null, caseDetails, String.class);
        } catch (CallbackException ex) {
            assertThat(ex.getMessage(), is("Callback to service has been unsuccessful for event " + caseEvent.getName()));
            throw ex;
        }
    }
}
