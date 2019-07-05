package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerErrorException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;

public class CallbackServiceTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ApplicationParams applicationParams;

    private CallbackService callbackService;

    private List<Integer> defaultCallbackRetryIntervals = Lists.newArrayList(0, 1, 3);
    private Integer defaultCallbackReadTimeout = 1000;
    private final String testUrl = "http://localhost:/test-callback";
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseEvent caseEvent = new CaseEvent();
    private final CallbackResponse callbackResponse = aCallbackResponse().build();
    private final ResponseEntity<CallbackResponse> response = ResponseEntity.ok(callbackResponse);

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        caseEvent.setId("TEST-EVENT");

        callbackResponse.setData(caseDetails.getData());

        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(response).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));
        doReturn(defaultCallbackRetryIntervals).when(applicationParams).getCallbackRetryIntervalsInSeconds();
        doReturn(defaultCallbackReadTimeout).when(applicationParams).getCallbackReadTimeoutInMillis();
        callbackService = new CallbackService(securityUtils, restTemplate, applicationParams);
    }

    @Nested
    @DisplayName("Default Retry Context")
    class DefaultRetryContext {
        @Test
        @DisplayName("Should return with no errors or warnings")
        public void shouldReturnWithNoErrorsOrWarnings() {
            final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> verify(restTemplate).setRequestFactory(any(HttpComponentsClientHttpRequestFactory.class)),
                () -> verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }

        @Test
        @DisplayName("Should retry if callback responds late")
        public void shouldRetryIfCallbackRespondsLate() {
            doThrow(RestClientException.class)
                .doThrow(RestClientException.class)
                .doReturn(response)
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));

            Instant start = Instant.now();
            callbackService.send(testUrl, null, caseEvent, caseDetails);

            final Duration between = Duration.between(start, Instant.now());
            Assertions.assertAll(
                () -> verify(restTemplate, times(3)).setRequestFactory(any(HttpComponentsClientHttpRequestFactory.class)),
                () -> verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class)),
                () -> verifyNoMoreInteractions(restTemplate),
                () -> assertThat((int) between.toMillis(), greaterThan(4000))
            );
        }
    }

    @Nested
    @DisplayName("Custom Retry Context")
    class CustomRetryContext {

        @Test
        @DisplayName("Should return with no errors or warnings and no retries configured")
        public void shouldReturnWithNoRetriesConfigured() {
            final Optional<CallbackResponse> result = callbackService.send(testUrl, Lists.newArrayList(500), caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(restTemplate).setRequestFactory(any(HttpComponentsClientHttpRequestFactory.class)),
                () -> verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }

        @Test
        @DisplayName("Should return with no errors or warnings and callback respond on second try")
        public void shouldReturnWithOneRetries() {
            doThrow(RestClientException.class)
                .doReturn(response)
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));

            final Optional<CallbackResponse> result = callbackService.send(testUrl, Lists.newArrayList(500, 1000), caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(restTemplate, times(2)).setRequestFactory(any(HttpComponentsClientHttpRequestFactory.class)),
                () -> verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }

        @Test
        @DisplayName("Should return with no errors or warnings and callback respond on third retry")
        public void shouldRetryIfCallbackRespondsAfterTwoRetries() {
            doThrow(RestClientException.class)
                .doThrow(RestClientException.class)
                .doReturn(response)
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));

            final Optional<CallbackResponse> result = callbackService.send(testUrl, Lists.newArrayList(500, 1000, 1500), caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(restTemplate, times(3)).setRequestFactory(any(HttpComponentsClientHttpRequestFactory.class)),
                () -> verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }
    }

    @Test
    public void shouldReturnWithErrorsOrWarningsIfExist() {
        callbackResponse.setErrors(Collections.singletonList("Test message"));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, caseDetails);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), contains("Test message"));
    }

    @Test
    public void shouldFailIfCallbackCallFailsForeverWithRestClientException() {
        doThrow(RestClientException.class)
            .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));

        CallbackException callbackException = assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
        assertThat(callbackException.getMessage(), is(equalTo("Unsuccessful callback to http://localhost:/test-callback")));
    }

    @Test
    public void shouldFailIfCallbackCallFailsFirstTimeWithNonRestClientException() {
        doThrow(ServerErrorException.class)
            .doReturn(response)
            .when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));

        assertThrows(ServerErrorException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
    }

    @Test
    public void shouldPassValidationIfNoCallbackErrorsAndWarnings()  {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
    }

    @Test
    public void shouldFailValidationIfWarningsAndDoNotIgnoreWarnings() {
        final String TEST_WARNING_1 = "WARNING 1";
        final String TEST_WARNING_2 = "WARNING 2";

        final List<String> warnings = new ArrayList<>();
        warnings.add(TEST_WARNING_1);
        warnings.add(TEST_WARNING_2);
        callbackResponse.setWarnings(warnings);

        ApiException apiException = assertThrows(ApiException.class, () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false));
        assertThat(apiException.getMessage(), is(equalTo("Unable to proceed because there are one or more callback Errors or Warnings")));
    }

    @Test
    public void shouldPassValidationIfWarningsAndIgnoreWarnings() {
        final String TEST_WARNING_1 = "WARNING 1";
        final String TEST_WARNING_2 = "WARNING 2";

        final List<String> warnings = new ArrayList<>();
        warnings.add(TEST_WARNING_1);
        warnings.add(TEST_WARNING_2);
        callbackResponse.setWarnings(warnings);

        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
    }

    @Test
    public void shouldFailValidationIfErrorsAndIgnoreWarnings() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));
        ApiException apiException = assertThrows(ApiException.class, () -> callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true));
        assertThat(apiException.getMessage(), is(equalTo("Unable to proceed because there are one or more callback Errors or Warnings")));
    }
}
