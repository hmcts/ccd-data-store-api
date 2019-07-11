package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.io.IOException;
import java.io.InputStream;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;

public class CallbackServiceTest {
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse closeableHttpResponse;
    @Mock
    private StatusLine statusLine;

    private CallbackService callbackService;

    private List<Integer> defaultCallbackRetryIntervals = Lists.newArrayList(0, 1, 3);
    private Integer defaultCallbackReadTimeout = 1000;
    private final String testUrl = "http://localhost:/test-callback";
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseEvent caseEvent = new CaseEvent();
    private HttpEntity httpEntity;
    private final CallbackResponse callbackResponse = aCallbackResponse().build();
    private final ResponseEntity<CallbackResponse> response = ResponseEntity.ok(callbackResponse);

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        caseEvent.setId("TEST-EVENT");

        callbackResponse.setData(caseDetails.getData());

        InputStream stubInputStream =
            IOUtils.toInputStream("{\"data\":{\"TextField0\":\"TextField0\"}}", "UTF-8");
        httpEntity = EntityBuilder.create().setStream(stubInputStream).build();

        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(closeableHttpResponse).when(httpClient).execute(any(HttpPost.class));
        doReturn(statusLine).when(closeableHttpResponse).getStatusLine();
        doReturn(httpEntity).when(closeableHttpResponse).getEntity();
        doReturn(200).when(statusLine).getStatusCode();
        doReturn(defaultCallbackRetryIntervals).when(applicationParams).getCallbackRetryIntervalsInSeconds();
        doReturn(defaultCallbackReadTimeout).when(applicationParams).getCallbackReadTimeoutInMillis();
        callbackService = new CallbackService(securityUtils, applicationParams, httpClient);
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
                () -> verify(httpClient).execute(any(HttpPost.class)),
                () -> verifyNoMoreInteractions(httpClient)
            );
        }

        @Test
        @DisplayName("Should retry if callback responds late")
        public void shouldRetryIfCallbackRespondsLate() throws IOException {
            doThrow(ClientProtocolException.class)
                .doThrow(ClientProtocolException.class)
                .doReturn(closeableHttpResponse)
                .when(httpClient).execute(any(HttpPost.class));

            Instant start = Instant.now();
            callbackService.send(testUrl, null, caseEvent, caseDetails);

            final Duration between = Duration.between(start, Instant.now());
            Assertions.assertAll(
                () -> verify(httpClient, times(3)).execute(any(HttpPost.class)),
                () -> verifyNoMoreInteractions(httpClient),
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
                () -> verify(httpClient).execute(any(HttpPost.class)),
                () -> verifyNoMoreInteractions(httpClient)
            );
        }

        @Test
        @DisplayName("Should return with no errors or warnings and callback respond on second try")
        public void shouldReturnWithOneRetries() throws IOException {
            doThrow(ClientProtocolException.class)
                .doReturn(closeableHttpResponse)
                .when(httpClient).execute(any(HttpPost.class));

            final Optional<CallbackResponse> result = callbackService.send(testUrl, Lists.newArrayList(500, 1000), caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(httpClient, times(2)).execute(any(HttpPost.class)),
                () -> verifyNoMoreInteractions(httpClient)
            );
        }

        @Test
        @DisplayName("Should return with no errors or warnings and callback respond on third retry")
        public void shouldRetryIfCallbackRespondsAfterTwoRetries() throws IOException {
            doThrow(ClientProtocolException.class)
                .doThrow(ClientProtocolException.class)
                .doReturn(closeableHttpResponse)
                .when(httpClient).execute(any(HttpPost.class));

            final Optional<CallbackResponse> result = callbackService.send(testUrl, Lists.newArrayList(500, 1000, 1500), caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(httpClient, times(3)).execute(any(HttpPost.class)),
                () -> verifyNoMoreInteractions(httpClient)
            );
        }
    }

    @Test
    public void shouldReturnWithErrorsOrWarningsIfExist() throws IOException {
        InputStream stubInputStream =
            IOUtils.toInputStream("{\"errors\":[\"Test message\"]}", "UTF-8");
        httpEntity = EntityBuilder.create().setStream(stubInputStream).build();
        doReturn(httpEntity).when(closeableHttpResponse).getEntity();

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, caseDetails);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), contains("Test message"));
    }

    @Test
    public void shouldFailIfCallbackCallFailsForeverWithRestClientException() throws IOException {
        doThrow(ClientProtocolException.class)
            .when(httpClient).execute(any(HttpPost.class));

        CallbackException callbackException = assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
        assertThat(callbackException.getMessage(), is(equalTo("Unsuccessful callback to http://localhost:/test-callback")));
    }

    @Test
    public void shouldFailIfCallbackCallFailsFirstTimeWithNonRestClientException() throws IOException {
        doThrow(IllegalArgumentException.class)
            .doReturn(closeableHttpResponse)
            .when(httpClient).execute(any(HttpPost.class));

        assertThrows(IllegalArgumentException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
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
