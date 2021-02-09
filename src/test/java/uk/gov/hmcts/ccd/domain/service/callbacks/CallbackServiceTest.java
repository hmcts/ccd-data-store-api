package uk.gov.hmcts.ccd.domain.service.callbacks;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseEventDefinition.setId("TEST-EVENT");

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        callbackResponse.setData(caseDetails.getData());

        initSecurityContext();
        callbackService = new CallbackService(securityUtils, restTemplate, applicationParams, appinsights);

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
    public void shouldSetIgnoreWarningsFlagInCallbackRequestIfSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, true);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(true)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if not set by client")
    public void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNotSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, false);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(false)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if null set by client")
    public void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNullSetByClient() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", nullValue()));
    }

    @Test
    @DisplayName("Should track callback event")
    public void shouldTrackCallbackEvent() throws Exception {
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);

        verify(appinsights).trackCallbackEvent(eq(CALLBACK_TYPE), eq(URL), eq("200"), any(Duration.class));
    }

    @Test
    @DisplayName("Should track callback event on exception")
    public void shouldTrackCallbackEventOnException() throws Exception {
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
    public void shouldLogAllCallbackEvent() throws Exception {
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
    public void shouldLogCallbackEventMultiple() throws Exception {
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
    public void shouldLogCallbackEvent() throws Exception {
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
    public void shouldNotLogCallbackEvent() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList();
        ccdCallbackLogControl.add("Notest-callback");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0,logsList.size());
    }

    @Test
    @DisplayName("Should Not Log callback event when empty")
    public void shouldNotLogCallbackEventEmpty() throws Exception {
        List<String> ccdCallbackLogControl = new ArrayList<String>();
        ccdCallbackLogControl.add("");
        doReturn(ccdCallbackLogControl).when(applicationParams).getCcdCallbackLogControl();
        callbackService.send(URL, CALLBACK_TYPE, caseEventDefinition, null, caseDetails, (Boolean)null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0,logsList.size());
    }

    private void initSecurityContext() {
        doReturn(principal).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

}
