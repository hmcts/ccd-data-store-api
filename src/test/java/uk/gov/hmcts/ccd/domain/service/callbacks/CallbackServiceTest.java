package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackServiceTest {

    public static final String URL = "/test-callback.*";
    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

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


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseEventDefinition.setId("TEST-EVENT");

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        callbackResponse.setData(caseDetails.getData());

        initSecurityContext();
        callbackService = new CallbackService(securityUtils, restTemplate);

        final ResponseEntity<CallbackResponse> responseEntity = new ResponseEntity<>(callbackResponse, HttpStatus.OK);
        when(restTemplate
            .exchange(eq(URL), eq(HttpMethod.POST), isA(HttpEntity.class), eq(CallbackResponse.class)))
            .thenReturn(responseEntity);
    }

    @Test
    @DisplayName("Should set ignore warning flag in callback request if set by client")
    public void shouldSetIgnoreWarningsFlagInCallbackRequestIfSetByClient() throws Exception {
        callbackService.send(URL, caseEventDefinition, null, caseDetails, true);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(true)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if not set by client")
    public void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNotSetByClient() throws Exception {
        callbackService.send(URL, caseEventDefinition, null, caseDetails, false);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", is(false)));
    }

    @Test
    @DisplayName("Should not set ignore warning flag in callback request if null set by client")
    public void shouldNotSetIgnoreWarningsFlagInCallbackRequestIfNullSetByClient() throws Exception {
        callbackService.send(URL, caseEventDefinition, null, caseDetails, (Boolean)null);

        verify(restTemplate).exchange(eq(URL), eq(HttpMethod.POST), argument.capture(), eq(CallbackResponse.class));
        assertThat(argument.getValue().getBody(), hasProperty("ignoreWarning", nullValue()));
    }

    private void initSecurityContext() {
        doReturn(principal).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

}
