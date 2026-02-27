package uk.gov.hmcts.ccd.domain.service.callbacks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackServiceTest {

    public static final String URL = "https://localhost/test-callback";
    public static final CallbackType CALLBACK_TYPE = CallbackType.ABOUT_TO_START;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private AppInsights appinsights;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Jwt principal;
    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<HttpEntity> argument;

    private CallbackService callbackService;

    private CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
    private CaseDetails caseDetails = new CaseDetails();
    private CallbackResponse callbackResponse = new CallbackResponse();
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    public static final String responseAttrJson1 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00003",
                    "task_name": "task name 3 from Attribute"
                },
                "complete_task": "false"
            }
        }
        """;
    public static final String responseAttrJson2 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000004",
                    "task_name": "task name 4 from Attribute"
                },
                "complete_task": "false"
            }
        }
        """;

    public static final String responseHdrJson1 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "00001",
                    "task_name": "task name 1 from hdr"
                },
                "complete_task": "false"
            }
        }
        """;
    public static final String responseHdrJson2 = """
        {
            "user_task": {
                "task_data": {
                    "task_id": "000002",
                    "task_name": "task name 2 from hdr"
                },
                "complete_task": "false"
            }
        }
        """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        caseEventDefinition.setId("TEST-EVENT");

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        callbackResponse.setData(caseDetails.getData());

        initSecurityContext();
        callbackService = new CallbackService(securityUtils, restTemplate, applicationParams, appinsights, request,
            objectMapper);

        final ResponseEntity<CallbackResponse> responseEntity = new ResponseEntity<>(callbackResponse, HttpStatus.OK);
        when(restTemplate
            .exchange(eq(URL), eq(HttpMethod.POST), isA(HttpEntity.class), eq(CallbackResponse.class)))
            .thenReturn(responseEntity);
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowedHttpHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("localhost"));
        when(applicationParams.getCcdCallbackLogControl()).thenReturn(List.of());

        logger = (Logger) LoggerFactory.getLogger(CallbackService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAndStopAllAppenders();
    }

    @Test
    @DisplayName("Should set ignore warning flag in callback request if set by client")
    void shouldSetIgnoreWarningsFlagInCallbackRequestIfSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, true);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(true)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if not set by client")
    void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNotSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, false);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(false)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if null set by client")
    void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNullSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", nullValue()));
    }

    @Test
    @DisplayName("Should not forward sensitive security headers to callback")
    void shouldNotForwardSensitiveSecurityHeadersToCallback() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, false);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        HttpHeaders headers = argument.getValue().getHeaders();
        assertTrue(headers.containsKey(SecurityUtils.SERVICE_AUTHORIZATION));
        assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION));
        assertFalse(headers.containsKey("user-id"));
        assertFalse(headers.containsKey("user-roles"));
    }

    @Test
    @DisplayName("Should reject callback URL when host not allowlisted")
    void shouldRejectCallbackHostWhenNotAllowlisted() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("trusted.example.com"));

        assertThrows(CallbackException.class, () ->
            callbackService.send("https://evil.example.com/callback", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );
    }

    @Test
    @DisplayName("Should reject callback URL when non-https host not in approved HTTP host list")
    void shouldRejectHttpCallbackHostWhenNotApproved() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowedHttpHosts()).thenReturn(List.of("localhost"));

        assertThrows(CallbackException.class, () ->
            callbackService.send("http://trusted.example.com/callback", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );
    }

    @Test
    @DisplayName("Should reject callback URL when resolving to localhost and private hosts are not approved")
    void shouldRejectLocalhostCallbackWhenPrivateHostNotApproved() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("trusted.example.com"));

        assertThrows(CallbackException.class, () ->
            callbackService.send("https://localhost/callback", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );
    }

    @Test
    @DisplayName("Should reject callback URL when host is IPv6 unique local address")
    void shouldRejectIpv6UlaCallbackWhenPrivateHostNotApproved() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("trusted.example.com"));

        CallbackException callbackException = assertThrows(CallbackException.class, () ->
            callbackService.send("https://[fd00::1]/callback", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );

        assertTrue(callbackException.getMessage().contains("private or local network address"));
    }

    @Test
    @DisplayName("Should reject callback URL when it includes embedded credentials")
    void shouldRejectCallbackUrlWithEmbeddedCredentials() {
        assertThrows(CallbackException.class, () ->
            callbackService.send("https://user:pass@localhost/callback", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );
    }

    @Test
    @DisplayName("Should redact callback URL query from validation exception message")
    void shouldRedactCallbackUrlQueryFromValidationException() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("trusted.example.com"));

        CallbackException callbackException = assertThrows(CallbackException.class, () ->
            callbackService.send("https://evil.example.com/callback?token=secret-value", CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );

        assertFalse(callbackException.getMessage().contains("secret-value"));
    }

    @Test
    @DisplayName("Should allow callback URL when host is allowlisted and HTTPS")
    void shouldAllowAllowlistedHttpsHost() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("localhost"));

        assertThatNoException().isThrownBy(() ->
            callbackService.send(URL, CALLBACK_TYPE,
                caseEventDefinition, null, caseDetails, false)
        );
    }

    @Test
    @DisplayName("Should track callback event")
    void shouldTrackCallbackEvent() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);

        verify(appinsights).trackCallbackEvent(eq(CALLBACK_TYPE), eq(URL), eq("200"), any(Duration.class));
    }

    @Test
    @DisplayName("Should track callback event on exception")
    void shouldTrackCallbackEventOnException() throws Exception {
        when(restTemplate
            .exchange(eq(URL), eq(HttpMethod.POST), isA(HttpEntity.class), eq(CallbackResponse.class)))
            .thenThrow(new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {
            });

        try {
            callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        } catch (CallbackException ex) {
            verify(appinsights).trackCallbackEvent(eq(CALLBACK_TYPE), eq(URL), eq("400"), any(Duration.class));
        }

    }

    @Test
    @DisplayName("Should LogAll callbacks")
    void shouldLogAllCallbackEvent() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList();
        ccdCallbackLogControl.add("*");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Invoking callback {} of type {} with request: {}", logsList.get(0)
            .getMessage());
    }

    @Test
    @DisplayName("Should Log callback test multiple callbacks")
    void shouldLogCallbackEventMultiple() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList<String>();
        ccdCallbackLogControl.add("abc-callback");
        ccdCallbackLogControl.add("xyz-callback");
        ccdCallbackLogControl.add("test-callback");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Callback {} response received: {}", logsList.get(1)
            .getMessage());
    }

    @Test
    @DisplayName("Should Log callback test single callbacks")
    void shouldLogCallbackEvent() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList();
        ccdCallbackLogControl.add("test-callback");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Invoking callback {} of type {} with request: {}", logsList.get(0)
            .getMessage());
        assertEquals("Callback {} response received: {}", logsList.get(1)
            .getMessage());
    }

    @Test
    @DisplayName("Should Not Log callback event")
    void shouldNotLogCallbackEvent() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList();
        ccdCallbackLogControl.add("Notest-callback");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0,logsList.size());
    }

    @Test
    @DisplayName("Should Not Log callback event when empty")
    void shouldNotLogCallbackEventEmpty() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList<String>();
        ccdCallbackLogControl.add("");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0,logsList.size());
    }

    @Test
    @DisplayName("Should add callback passthru headers from request header")
    void shouldAddCallbackPassthruHeadersFromRequestHeader() throws Exception {
        List<String> customHeaders = List.of("Client-Context","Dummy-Context1","DummyContext-2");
        List<String> customHeaderValues = List.of("{json1:{test:1221}}","{json2:{test:2332}}");

        when(applicationParams.getCallbackPassthruHeaderContexts()).thenReturn(customHeaders);
        when(request.getHeader(customHeaders.get(0))).thenReturn(customHeaderValues.get(0));
        when(request.getHeader(customHeaders.get(1))).thenReturn(customHeaderValues.get(1));
        when(request.getHeader(customHeaders.get(2))).thenReturn(null);

        HttpHeaders httpHeaders = new HttpHeaders();
        callbackService.addPassThroughHeaders(httpHeaders);

        assertEquals(2, httpHeaders.size());
        assertTrue(httpHeaders.containsKey(customHeaders.get(0)));
        assertEquals(customHeaderValues.get(0), httpHeaders.get(customHeaders.get(0)).get(0));
        assertTrue(httpHeaders.containsKey(customHeaders.get(1)));
        assertEquals(customHeaderValues.get(1), httpHeaders.get(customHeaders.get(1)).get(0));
        assertFalse(httpHeaders.containsKey(customHeaders.get(2)));
    }

    @Test
    @DisplayName("Should block sensitive callback passthru header contexts")
    void shouldBlockSensitiveCallbackPassthruHeaderContexts() {
        List<String> customHeaders = List.of("Client-Context", "Authorization", "user-id", "ServiceAuthorization");
        when(applicationParams.getCallbackPassthruHeaderContexts()).thenReturn(customHeaders);
        when(request.getHeader("Client-Context")).thenReturn("{ctx:true}");
        when(request.getHeader("Authorization")).thenReturn("Bearer leaked");
        when(request.getHeader("user-id")).thenReturn("u123");
        when(request.getHeader("ServiceAuthorization")).thenReturn("s2s-token");

        HttpHeaders httpHeaders = new HttpHeaders();
        callbackService.addPassThroughHeaders(httpHeaders);

        assertTrue(httpHeaders.containsKey("Client-Context"));
        assertFalse(httpHeaders.containsKey("Authorization"));
        assertFalse(httpHeaders.containsKey("user-id"));
        assertFalse(httpHeaders.containsKey("ServiceAuthorization"));
    }

    @Test
    @DisplayName("Should add callback passthru headers from request attribute")
    void shouldAddCallbackPassthruHeadersFromRequestAttribute() throws Exception {
        List<String> customHeaders = List.of("Client-Context","Dummy-Context1","DummyContext-2");
        JSONObject responseAttr1 = new JSONObject(responseAttrJson1);
        JSONObject responseAttr2 = new JSONObject(responseAttrJson2);
        JSONObject responseHdr1 = new JSONObject(responseHdrJson1);
        JSONObject responseHdr2 = new JSONObject(responseHdrJson2);
        List<String> customHeaderValues = List.of(ClientContextUtil.encodeToBase64(responseAttr1.toString()),
            ClientContextUtil.encodeToBase64(responseAttr2.toString()));

        when(applicationParams.getCallbackPassthruHeaderContexts()).thenReturn(customHeaders);
        when(request.getAttribute(customHeaders.get(0))).thenReturn(customHeaderValues.get(0));
        when(request.getAttribute(customHeaders.get(1))).thenReturn(customHeaderValues.get(1));
        when(request.getAttribute(customHeaders.get(2))).thenReturn(null);
        when(request.getHeader(customHeaders.get(0)))
            .thenReturn(ClientContextUtil.encodeToBase64(responseHdr1.toString()));
        when(request.getHeader(customHeaders.get(1)))
            .thenReturn(ClientContextUtil.encodeToBase64(responseHdr2.toString()));
        when(request.getHeader(customHeaders.get(2))).thenReturn(null);

        HttpHeaders httpHeaders = new HttpHeaders();
        callbackService.addPassThroughHeaders(httpHeaders);

        assertEquals(2, httpHeaders.size());
        assertTrue(httpHeaders.containsKey(customHeaders.get(0)));
        assertEquals(customHeaderValues.get(0), httpHeaders.get(customHeaders.get(0)).get(0));
        assertTrue(httpHeaders.containsKey(customHeaders.get(1)));
        assertEquals(customHeaderValues.get(1), httpHeaders.get(customHeaders.get(1)).get(0));
        assertFalse(httpHeaders.containsKey(customHeaders.get(2)));
    }

    @Test
    @DisplayName("Should not throw ApiException when no error or warning fields set in response")
    void shouldNotThrowApiExceptionWhenNoErrorOrWarningFieldsSet() {
        assertThatNoException().isThrownBy(
            () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false)
        );
    }

    @Test
    @DisplayName("Should not throw ApiException when only warning fields set in response and warnings ignored")
    void shouldNotThrowApiExceptionWhenIgnorableWarningsSet() {
        callbackResponse.setWarnings(List.of("Warning 1"));

        assertThatNoException().isThrownBy(
            () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true)
        );
    }

    @Test
    @DisplayName("Should throw ApiException when only warning fields set in response and warnings not ignored")
    void shouldThrowApiExceptionWhenNonIgnorableWarningsSet() {
        List<String> expectedWarnings = List.of("Warning 1");
        callbackResponse.setWarnings(expectedWarnings);

        Throwable throwable = catchThrowable(
            () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false)
        );

        Assertions.assertThat(throwable)
            .isInstanceOf(ApiException.class)
            .hasMessage("Unable to proceed because there are one or more callback Errors or Warnings");

        ApiException apiException = (ApiException) throwable;
        Assertions.assertThat(apiException.getCallbackWarnings()).isEqualTo(expectedWarnings);
        Assertions.assertThat(apiException.getCallbackErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ApiException when only error fields set in response")
    void shouldThrowApiExceptionWhenErrorsSet() {
        List<String> expectedErrors = List.of("Error 1");
        callbackResponse.setErrors(expectedErrors);

        Throwable throwable = catchThrowable(
            () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false)
        );

        Assertions.assertThat(throwable)
            .isInstanceOf(ApiException.class)
            .hasMessage("Unable to proceed because there are one or more callback Errors or Warnings");

        ApiException apiException = (ApiException) throwable;
        Assertions.assertThat(apiException.getCallbackErrors()).isEqualTo(expectedErrors);
        Assertions.assertThat(apiException.getCallbackWarnings()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ApiException with custom error message")
    void shouldThrowApiExceptionWithCustomMessageWhenErrorMessageOverrideSet() {
        String expectedErrorMessage = "Some custom error message";
        callbackResponse.setErrorMessageOverride(expectedErrorMessage);

        Throwable throwable = catchThrowable(
            () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false)
        );

        Assertions.assertThat(throwable)
            .isInstanceOf(ApiException.class)
            .hasMessage(expectedErrorMessage);

        ApiException apiException = (ApiException) throwable;
        Assertions.assertThat(apiException.getCallbackErrors()).isEmpty();
        Assertions.assertThat(apiException.getCallbackWarnings()).isEmpty();
    }

    private void initSecurityContext() {
        doReturn(principal).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

}
