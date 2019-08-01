package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import org.apache.http.StatusLine;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.io.IOException;
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
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;

public class CallbackServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    public static final ArrayList<Integer> NON_DISABLE_CUSTOM_RETRIES = Lists.newArrayList(1, 3, 5);
    public static final ArrayList<Integer> DISABLE_CUSTOM_RETRIES = Lists.newArrayList(0);
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private StatusLine statusLine;

    private CallbackService callbackService;

    private List<Integer> defaultCallbackRetryIntervals = Lists.newArrayList(0, 1, 3);
    private Integer defaultCallbackReadTimeout = 1000;
    private final String testUrl = "http://localhost:/test-callback";
    private final CaseDetails caseDetails = new CaseDetails();
    private final CaseEvent caseEvent = new CaseEvent();
    private final CallbackResponse callbackResponse = aCallbackResponse().build();
    private ResponseEntity responseEntity;
    private final ResponseEntity<CallbackResponse> response = ResponseEntity.ok(callbackResponse);

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        caseDetails.setState("test state");
        caseDetails.setCaseTypeId("test case type");

        caseEvent.setId("TEST-EVENT");

        callbackResponse.setData(caseDetails.getData());

        JsonNode textField0 = getTextNode("TextField0");
        List<JsonNode> textFieldList = Lists.newArrayList();
        textFieldList.add(textField0);
        responseEntity = ResponseEntity.<CallbackResponse>ok(aCallbackResponse()
            .withDataClassification(aClassificationBuilder()
                .withData("TextField0", textFieldList)
                .buildAsMap())
            .build());

        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(responseEntity).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CallbackResponse.class));
        doReturn(200).when(statusLine).getStatusCode();
        doReturn(defaultCallbackRetryIntervals).when(applicationParams).getCallbackRetryIntervalsInSeconds();
        doReturn(defaultCallbackReadTimeout).when(applicationParams).getCallbackReadTimeoutInMillis();
        callbackService = new CallbackService(securityUtils, applicationParams, restTemplate);
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
                () -> verify(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }

        @Test
        @DisplayName("Should retry if callback responds late")
        public void shouldRetryIfCallbackRespondsLate() throws IOException {
            doThrow(RestClientException.class)
                .doThrow(RestClientException.class)
                .doReturn(responseEntity)
                .when(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

            Instant start = Instant.now();
            callbackService.send(testUrl, null, caseEvent, caseDetails);

            final Duration between = Duration.between(start, Instant.now());
            Assertions.assertAll(
                () -> verify(restTemplate, times(3)).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)),
                () -> verifyNoMoreInteractions(restTemplate),
                () -> assertThat((int) between.toMillis(), greaterThan(4000))
            );
        }
    }

    @Nested
    @DisplayName("Custom Retry Context")
    class CustomRetryContext {

        @Test
        @DisplayName("Should disable callbacks")
        public void shouldDisableCallbacks() {
            final Optional<CallbackResponse> result = callbackService.send(testUrl, DISABLE_CUSTOM_RETRIES, caseEvent, caseDetails);
            final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

            Assertions.assertAll(
                () -> assertTrue(response.getErrors().isEmpty()),
                () -> assertTrue(response.getWarnings().isEmpty()),
                () -> verify(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)),
                () -> verifyNoMoreInteractions(restTemplate)
            );
        }

        @Test
        @DisplayName("Should execute default callback flow if value other than 0 provided")
        public void shouldExecuteDefaultCallbackFlow() {
            doThrow(RestClientException.class)
                .doThrow(RestClientException.class)
                .doReturn(responseEntity)
                .when(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

            Instant start = Instant.now();
            callbackService.send(testUrl, NON_DISABLE_CUSTOM_RETRIES, caseEvent, caseDetails);

            final Duration between = Duration.between(start, Instant.now());
            Assertions.assertAll(
                () -> verify(restTemplate, times(3)).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)),
                () -> verifyNoMoreInteractions(restTemplate),
                () -> assertThat((int) between.toMillis(), greaterThan(4000))
            );
        }
    }

    @Test
    public void shouldReturnWithErrorsOrWarningsIfExist() throws IOException {
        responseEntity = ResponseEntity.<CallbackResponse>ok(aCallbackResponse().withError("Test message").build());
        doReturn(responseEntity).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

        final Optional<CallbackResponse> result = callbackService.send(testUrl, null, caseEvent, caseDetails);

        final CallbackResponse response = result.orElseThrow(() -> new AssertionError("Missing result"));

        assertThat(response.getErrors(), contains("Test message"));
    }

    @Test
    public void shouldFailIfCallbackCallFailsForeverWithRestClientException() throws IOException {
        doThrow(RestClientException.class)
            .when(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

        CallbackException callbackException = assertThrows(CallbackException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
        assertThat(callbackException.getMessage(), is(equalTo("Unsuccessful callback to http://localhost:/test-callback")));
    }

    @Test
    public void shouldFailIfCallbackCallFailsFirstTimeWithNonRestClientException() throws IOException {
        doThrow(IllegalStateException.class)
            .doReturn(responseEntity)
            .when(restTemplate).exchange(eq(testUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

        assertThrows(IllegalStateException.class, () -> callbackService.send(testUrl, null, caseEvent, caseDetails));
    }

    @Test
    public void shouldPassValidationIfNoCallbackErrorsAndWarnings() {
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

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }

}
