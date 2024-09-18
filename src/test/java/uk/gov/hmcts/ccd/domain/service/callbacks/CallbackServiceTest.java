package uk.gov.hmcts.ccd.domain.service.callbacks;

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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackServiceTest {

    public static final String URL = "/test-callback.*";
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

    @Captor
    private ArgumentCaptor<HttpEntity> argument;

    private CallbackService callbackService;

    private CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
    private CaseDetails caseDetails = new CaseDetails();
    private CallbackResponse callbackResponse = new CallbackResponse();
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    public static final JSONObject responseAttrJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "00003",
                    "task_name": "task name 3 from Attribute"
                },
                "complete_task": "false"
            }
        }
        """);
    public static final JSONObject responseAttrJson2 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "000004",
                    "task_name": "task name 4 from Attribute"
                },
                "complete_task": "false"
            }
        }
        """);

    public static final JSONObject responseHdrJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "00001",
                    "task_name": "task name 1 from hdr"
                },
                "complete_task": "false"
            }
        }
        """);
    public static final JSONObject responseHdrJson2 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "000002",
                    "task_name": "task name 2 from hdr"
                },
                "complete_task": "false"
            }
        }
        """);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseEventDefinition.setId("TEST-EVENT");

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        callbackResponse.setData(caseDetails.getData());

        initSecurityContext();
        callbackService = new CallbackService(securityUtils, restTemplate, applicationParams, appinsights, request);

        final ResponseEntity<CallbackResponse> responseEntity = new ResponseEntity<>(callbackResponse, HttpStatus.OK);
        when(restTemplate
            .exchange(eq(URL), eq(HttpMethod.POST), isA(HttpEntity.class), eq(CallbackResponse.class)))
            .thenReturn(responseEntity);

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
    @DisplayName("Should add callback passthru headers from request attribute")
    void shouldAddCallbackPassthruHeadersFromRequestAttribute() throws Exception {
        List<String> customHeaders = List.of("Client-Context","Dummy-Context1","DummyContext-2");
        List<String> customHeaderValues = List.of(ClientContextUtil.encodeToBase64(responseAttrJson1.toString()),
            ClientContextUtil.encodeToBase64(responseAttrJson2.toString()));

        when(applicationParams.getCallbackPassthruHeaderContexts()).thenReturn(customHeaders);
        when(request.getAttribute(customHeaders.get(0))).thenReturn(customHeaderValues.get(0));
        when(request.getAttribute(customHeaders.get(1))).thenReturn(customHeaderValues.get(1));
        when(request.getAttribute(customHeaders.get(2))).thenReturn(null);
        when(request.getHeader(customHeaders.get(0)))
            .thenReturn(ClientContextUtil.encodeToBase64(responseHdrJson1.toString()));
        when(request.getHeader(customHeaders.get(1)))
            .thenReturn(ClientContextUtil.encodeToBase64(responseHdrJson2.toString()));
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

    private void initSecurityContext() {
        doReturn(principal).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

}
