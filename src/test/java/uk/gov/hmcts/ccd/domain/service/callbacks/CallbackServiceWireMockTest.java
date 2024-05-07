package uk.gov.hmcts.ccd.domain.service.callbacks;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.WireMockBaseTest;

@TestPropertySource(properties =
    {
    "ccd.callback.timeouts=1,2,3"
    })
public class CallbackServiceWireMockTest extends WireMockBaseTest {
    private MockHttpServletRequest request;
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String HDR_CLIENT_CONTEXT = "Client-Context";
    public static final CallbackType TEST_CALLBACK_ABOUT_TO_START = CallbackType.ABOUT_TO_START;
    public static final CallbackType TEST_CALLBACK_ABOUT_TO_SUBMIT = CallbackType.ABOUT_TO_SUBMIT;
    public static final CallbackType TEST_CALLBACK_SUBMITTED = CallbackType.SUBMITTED;
    public static final JSONObject responseJson1 = new JSONObject("""
        {
            user_task: {
                task_data: task structure as retrieved from task management API,
                complete_task: false
            }
        }
        """);
    public static final JSONObject responseJson2 = new JSONObject("""
        {
            user_task: {
                task_data: task structure updated on 2nd call,
                complete_task: false
            }
        }
        """);
    public static final JSONObject responseJson3 = new JSONObject("""
        {
            user_task: {
                task_data: task structure updated on final call,
                complete_task: true
            }
        }
        """);

    @Test
    public void happyPathWithNoErrorsOrWarnings() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition,
            null, caseDetails, false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void passThruCustomHeadersLikeClientContext() throws Exception {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        final String testUrl1 = "http://localhost:" + wiremockPort + "/test-callback101";
        final String testUrl2 = "http://localhost:" + wiremockPort + "/test-callback102";
        final String testUrl3 = "http://localhost:" + wiremockPort + "/test-callback103";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback101.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(HDR_CLIENT_CONTEXT, responseJson1.toString())));

        stubFor(post(urlMatching("/test-callback102.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(HDR_CLIENT_CONTEXT, responseJson2.toString())));

        stubFor(post(urlMatching("/test-callback103.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(HDR_CLIENT_CONTEXT, responseJson3.toString())));

        final Optional<CallbackResponse> result1 = callbackService.send(testUrl1, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response1 = result1.orElseThrow(() -> new AssertionError("Missing result"));

        final Optional<CallbackResponse> result2 = callbackService.send(testUrl2, TEST_CALLBACK_ABOUT_TO_SUBMIT,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response2 = result2.orElseThrow(() -> new AssertionError("Missing result"));

        final Optional<CallbackResponse> result3 = callbackService.send(testUrl3, TEST_CALLBACK_SUBMITTED,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response3 = result3.orElseThrow(() -> new AssertionError("Missing result"));

        // Need a good Assertion here ... header is ...
        Object objectContext = request.getAttribute(HDR_CLIENT_CONTEXT);
        ArrayList<String> contextArray = (ArrayList<String>) objectContext;
        assertEquals(responseJson3.toString(), contextArray.get(0));
    }

    @org.junit.Ignore // TODO investigating socket issues in Azure
    @Test
    public void shouldRetryIfCallbackRespondsLate() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200).withFixedDelay(1500)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition,
            null, caseDetails,false);

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

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Test message"));
        callbackResponse.setData(caseDetails.getData());
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition,
            null, caseDetails,false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), Matchers.contains("Test message"));
    }

    @Test(expected = CallbackException.class)
    public void notFoundFailurePath() {
        final String testUrl = "http://localhost";
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
    }

    @Test(expected = CallbackException.class)
    public void serverError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
    }

    @Test
    public void retryOnServerError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callbackGrrrr";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
        } catch (Exception e) {
            e.printStackTrace();
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
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(401)));

        callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
    }

    @Test
    public void validateCallbackErrorsAndWarningsHappyPath() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithWarnings() {
        final String testWarning1 = "WARNING 1";
        final String testWarning2 = "WARNING 2";

        final CallbackResponse callbackResponse = new CallbackResponse();
        final List<String> warnings = new ArrayList<>();
        warnings.add(testWarning1);
        warnings.add(testWarning2);

        callbackResponse.setWarnings(warnings);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithErrorsAndIgnore() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));
        callbackResponse.setWarnings(Collections.singletonList("a warning"));
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
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
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback-submitted";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-submitted.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(201)));

        final ResponseEntity<String> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition,
            null, caseDetails, String.class);

        assertAll(
            () -> assertThat(result.getStatusCodeValue(), is(201)),
            () -> JSONAssert.assertEquals(
                "{\"data\":null,\"errors\":[],\"warnings\":[],\"data_classification\":null,\"security_classification\""
                    + ":null}",
                result.getBody(),
                JSONCompareMode.LENIENT)
        );
    }

    @Test
    public void shouldRetryOnError() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback-invaliddd";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-invaliddd.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callback-invaliddd.*")));
    }

    @Test(expected = CallbackException.class)
    public void shouldThrowCallbackException_whenSendInvalidUrlGetGenericBody() {
        final String testUrl = "http://localhost/invalid-test-callback";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ApplicationParams applicationParams = Mockito.mock(ApplicationParams.class);
        given(applicationParams.getCallbackRetries()).willReturn(Arrays.asList(3, 5));
        given(restTemplate.exchange(anyString(), eq(POST), isA(HttpEntity.class), eq(String.class)))
            .willThrow(new RestClientException("Fail to process"));

        // Builds a new callback service to avoid wiremock exception to get in the way
        final CallbackService underTest = new CallbackService(Mockito.mock(SecurityUtils.class), restTemplate,
            Mockito.mock(ApplicationParams.class), Mockito.mock(AppInsights.class),
            Mockito.mock(HttpServletRequest.class));
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        try {
            underTest.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null, caseDetails, String.class);
        } catch (CallbackException ex) {
            assertThat(ex.getMessage(), is("Callback to service has been unsuccessful for event "
                + caseEventDefinition.getName()));
            throw ex;
        }
    }
}
