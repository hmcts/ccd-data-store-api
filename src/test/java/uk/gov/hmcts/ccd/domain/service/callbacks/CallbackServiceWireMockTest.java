package uk.gov.hmcts.ccd.domain.service.callbacks;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.POST;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService.CLIENT_CONTEXT;

@TestPropertySource(properties =
    {
    "ccd.callback.timeouts=1,2,3"
    })
public class CallbackServiceWireMockTest extends WireMockBaseTest {
    private MockHttpServletRequest request;
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final CallbackType TEST_CALLBACK_ABOUT_TO_START = CallbackType.ABOUT_TO_START;
    public static final CallbackType TEST_CALLBACK_ABOUT_TO_SUBMIT = CallbackType.ABOUT_TO_SUBMIT;
    public static final CallbackType TEST_CALLBACK_SUBMITTED = CallbackType.SUBMITTED;
    public static final String requestJson1 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00000",
                    "task_name": "Initialise"
                }
            }
        }
        """;
    public static final String responseJson1 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00001",
                    "task_name": "task name 1"
                }
            },
            "complete_task": "false"
        }
        """;
    public static final String responseJson1Merged = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00001",
                    "task_name": "task name 1"
                }
            },
            "complete_task": "false"
        }
        """;
    public static final String requestJson2 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00002",
                    "task_name": "Step 2"
                }
            },
            "complete_task": "false"
        }
        """;
    public static final String responseJson2 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000002",
                    "task_name": "task name 2"
                }
            },
            "new_field": "check1",
            "complete_task": "false"
        }
        """;
    public static final String responseJson2Merged = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000002",
                    "task_name": "task name 2"
                }
            },
            "new_field": "check1",
            "complete_task": "false"
        }
        """;
    public static final String requestJson3 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00003",
                    "task_name": "Step 3 started"
                }
            },
            "new_field": "check1",
            "new_field2": "check2",
            "complete_task": "false"
        }
        """;
    public static final String responseJson3 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000003",
                    "task_name": "task name 3 completed"
                }
            },
            "new_field2": "check2ed",
            "complete_task": "true"
        }
        """;
    public static final String responseJson3Merged = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000003",
                    "task_name": "task name 3 completed"
                }
            },
            "new_field": "check1",
            "new_field2": "check2ed",
            "complete_task": "true"
        }
        """;

    @Test
    public void happyPathWithNoErrorsOrWarnings() throws Exception {
        final String testUrl = hostUrl + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void passThruCustomHeadersLikeClientContext() throws Exception {

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        final String testUrl1 = hostUrl + "/test-callback101";
        final String testUrl2 = hostUrl + "/test-callback102";
        final String testUrl3 = hostUrl + "/test-callback103";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        String clientContextValue1 = ClientContextUtil.encodeToBase64(new JSONObject(requestJson1).toString());
        stubFor(post(urlMatching("/test-callback101.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(CLIENT_CONTEXT, ClientContextUtil.encodeToBase64(
                        new JSONObject(responseJson1).toString()
                    ))));

        String clientContextValue2 = ClientContextUtil.encodeToBase64(new JSONObject(requestJson2).toString());
        stubFor(post(urlMatching("/test-callback102.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(CLIENT_CONTEXT, ClientContextUtil.encodeToBase64(
                        new JSONObject(responseJson2).toString()
                    ))));

        stubFor(post(urlMatching("/test-callback103.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse))
                .withStatus(200)
                .withHeader(CLIENT_CONTEXT, ClientContextUtil.encodeToBase64((
                        new JSONObject(responseJson3).toString())
                    ))));

        final Optional<CallbackResponse> result1 = callbackService.send(testUrl1, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response1 = result1.orElseThrow(() -> new AssertionError("Missing result"));
        assertOnRequestAttribute(new JSONObject(responseJson1Merged), CLIENT_CONTEXT);

        final Optional<CallbackResponse> result2 = callbackService.send(testUrl2, TEST_CALLBACK_ABOUT_TO_SUBMIT,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response2 = result2.orElseThrow(() -> new AssertionError("Missing result"));
        assertOnRequestAttribute(new JSONObject(responseJson2Merged), CLIENT_CONTEXT);

        final Optional<CallbackResponse> result3 = callbackService.send(testUrl3, TEST_CALLBACK_SUBMITTED,
            caseEventDefinition, null, caseDetails, false);
        final CallbackResponse response3 = result3.orElseThrow(() -> new AssertionError("Missing result"));
        assertOnRequestAttribute(new JSONObject(responseJson3Merged), CLIENT_CONTEXT);
    }

    @Disabled // TODO investigating socket issues in Azure
    @Test
    public void shouldRetryIfCallbackRespondsLate() throws Exception {
        final String testUrl = hostUrl + "/test-callback";

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(caseDetails.getData());

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)
                .withFixedDelay(1500)));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails,false);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));
        verify(exactly(2), postRequestedFor(urlMatching("/test-callback.*")));
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void failurePathWithErrors() throws Exception {
        final String testUrl = hostUrl + "/test-callback";

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

        final Optional<CallbackResponse> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails,false);
        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), Matchers.contains("Test message"));
    }

    @Test
    public void notFoundFailurePath() {
        final String testUrl = "http://localhost";
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");
        assertThrows(CallbackException.class, () ->
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null,
                caseDetails, false)
        );
    }

    @Test
    public void serverError() throws Exception {
        final String testUrl = hostUrl + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        assertThrows(CallbackException.class, () ->
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null,
                caseDetails, false)
        );
    }

    @Test
    public void retryOnServerError() throws Exception {
        final String testUrl = hostUrl + "/test-callbackGrrrr";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null,
                caseDetails, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void authorisationError() throws Exception {
        final String testUrl = hostUrl + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(401)));

        assertThrows(CallbackException.class, () ->
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null,
                caseDetails, false)
        );
    }

    @Test
    public void validateCallbackErrorsAndWarningsHappyPath() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithWarnings() {
        final String testWarning1 = "WARNING 1";
        final String testWarning2 = "WARNING 2";

        final CallbackResponse callbackResponse = new CallbackResponse();
        final List<String> warnings = new ArrayList<>();
        warnings.add(testWarning1);
        warnings.add(testWarning2);

        callbackResponse.setWarnings(warnings);
        assertThrows(ApiException.class, () ->
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false)
        );
        
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithErrorsAndIgnore() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));
        callbackResponse.setWarnings(Collections.singletonList("a warning"));
        assertThrows(ApiException.class, () ->
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true)
        );
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
        final String testUrl = hostUrl + "/test-callback-submitted";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-submitted.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(201)));

        final ResponseEntity<String> result = callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START,
            caseEventDefinition, null, caseDetails, String.class);

        assertAll(
            () -> assertThat(result.getStatusCode().value(), is(201)),
            () -> JSONAssert.assertEquals(
                "{\"data\":null,\"errors\":[],\"warnings\":[],\"data_classification\":null,\""
                    + "security_classification\"" + ":null}",
                result.getBody(),
                JSONCompareMode.LENIENT)
        );
    }

    @Test
    public void shouldRetryOnError() throws Exception {
        final String testUrl = hostUrl + "/test-callback-invaliddd";
        final CallbackResponse callbackResponse = new CallbackResponse();
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        stubFor(post(urlMatching("/test-callback-invaliddd.*")).willReturn(
            okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500)));

        Instant start = Instant.now();
        try {
            callbackService.send(testUrl, TEST_CALLBACK_ABOUT_TO_START, caseEventDefinition, null,
                caseDetails, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Duration between = Duration.between(start, Instant.now());
        assertThat((int) between.toMillis(), greaterThan(4000));
        verify(exactly(3), postRequestedFor(urlMatching("/test-callback-invaliddd.*")));
    }

    @Test
    public void shouldThrowCallbackException_whenSendInvalidUrlGetGenericBody() {
        final String testUrl = "http://localhost/invalid-test-callback";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ApplicationParams applicationParams = Mockito.mock(ApplicationParams.class);
        given(applicationParams.getCallbackRetries()).willReturn(Arrays.asList(3, 5));
        given(applicationParams.getCallbackAllowedHosts()).willReturn(List.of("localhost"));
        given(applicationParams.getCallbackAllowedHttpHosts()).willReturn(List.of("localhost"));
        given(applicationParams.getCallbackAllowPrivateHosts()).willReturn(List.of("localhost"));
        given(applicationParams.getCallbackPassthruHeaderContexts()).willReturn(Collections.emptyList());
        given(applicationParams.getCcdCallbackLogControl()).willReturn(Collections.emptyList());
        given(restTemplate.exchange(anyString(), eq(POST), isA(HttpEntity.class), eq(String.class)))
            .willThrow(new RestClientException("Fail to process"));

        // Builds a new callback service to avoid wiremock exception to get in the way
        final CallbackService underTest = new CallbackService(Mockito.mock(SecurityUtils.class), restTemplate,
            applicationParams, Mockito.mock(AppInsights.class),
            Mockito.mock(HttpServletRequest.class), Mockito.mock(ObjectMapper.class));
        final CaseDetails caseDetails = new CaseDetails();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("TEST-EVENT");

        CallbackException ex = assertThrows(CallbackException.class, () -> 
            underTest.send(testUrl, TEST_CALLBACK_ABOUT_TO_START,
                caseEventDefinition, null, caseDetails, String.class));
        assertThat(ex.getMessage(), is("Callback to service has been unsuccessful for event "
            + caseEventDefinition.getName()));
    }

    private void assertOnRequestAttribute(JSONObject jsonObject, String customContext) {
        Object objectContext = request.getAttribute(customContext);
        String contextValue = (String) objectContext;
        JsonNode jsonNode1 = null;
        JsonNode jsonNode2 = null;

        try {
            jsonNode1 = mapper.readTree(jsonObject.toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {
            jsonNode2 = mapper.readTree(ClientContextUtil.decodeFromBase64(contextValue));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        assertEquals(jsonNode1, jsonNode2);
    }

}
