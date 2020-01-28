package uk.gov.hmcts.ccd.domain.service.callbacks;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponse;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponseElement;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackFailureWithAssertForUpstreamException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final int ALLOWED_ASSERT_UPSTREAM_1 = 417;
    private static final int ALLOWED_ASSERT_UPSTREAM_2 = 423;
    private static final int AUTO_ASSERT_UPSTREAM_1 = 400;
    private static final int AUTO_ASSERT_UPSTREAM_402 = 402; // i.e. PaymentRequired special case
    private static final int ASSERT_UPSTREAM_NOT_PERMITTED = 418;

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

        // set AssertUpstreamList to known values
        final ApplicationParams applicationParams = Mockito.mock(ApplicationParams.class);
        Mockito.when(applicationParams.getCallbackStatusAllowedAssertUpstreamList())
            .thenReturn(Arrays.asList(ALLOWED_ASSERT_UPSTREAM_1, ALLOWED_ASSERT_UPSTREAM_2, AUTO_ASSERT_UPSTREAM_1, AUTO_ASSERT_UPSTREAM_402));
        Mockito.when(applicationParams.getCallbackStatusAutoAssertUpstreamList())
            .thenReturn(Arrays.asList(AUTO_ASSERT_UPSTREAM_1, AUTO_ASSERT_UPSTREAM_402));
        ReflectionTestUtils.setField(callbackService, "applicationParams", applicationParams);

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

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void failurePathWithAssertUpstream_permitted_withoutCallbackCR() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();

        callbackResponse.setAssertForUpstream(ALLOWED_ASSERT_UPSTREAM_1);

        final int expectedStatusCode = callbackResponse.getAssertForUpstream();
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // verify message matches CCD response element
            assertThat(ex.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            assertNull(exceptionCatalogueResponse.getDetails());

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void failurePathWithAssertUpstream_permitted_withCallbackCR_blankMessage() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();

        // callback CR
        final String expectedCatalogueResponseDetailsCode = "TEST.1001";
        final String expectedCatalogueResponseDetailsMessage = "";
        final CatalogueResponse callbackCatalogueResponse =
            generateCustomCatalogueResponse(expectedCatalogueResponseDetailsCode, expectedCatalogueResponseDetailsMessage);
        callbackResponse.setCatalogueResponse(callbackCatalogueResponse);

        callbackResponse.setAssertForUpstream(ALLOWED_ASSERT_UPSTREAM_1);

        final int expectedStatusCode = callbackResponse.getAssertForUpstream();
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // verify message matches CCD response element (as callback CR message blank)
            assertThat(ex.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            assertTrue(EqualsBuilder.reflectionEquals(exceptionCatalogueResponse.getDetails().get("callbackCatalogueResponse"), callbackCatalogueResponse));

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void failurePathWithAssertUpstream_permitted_withCallbackCR_nonBlankMessage() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();

        // callback CR
        final String expectedCatalogueResponseDetailsCode = "TEST.1001";
        final String expectedCatalogueResponseDetailsMessage = "Testing callback message";
        final CatalogueResponse callbackCatalogueResponse =
            generateCustomCatalogueResponse(expectedCatalogueResponseDetailsCode, expectedCatalogueResponseDetailsMessage);
        callbackResponse.setCatalogueResponse(callbackCatalogueResponse);

        callbackResponse.setAssertForUpstream(ALLOWED_ASSERT_UPSTREAM_1);

        final int expectedStatusCode = callbackResponse.getAssertForUpstream();
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // verify message matches callback CR
            assertThat(ex.getMessage(), is(callbackCatalogueResponse.getMessage()));

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            assertTrue(EqualsBuilder.reflectionEquals(exceptionCatalogueResponse.getDetails().get("callbackCatalogueResponse"), callbackCatalogueResponse));

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = ApiException.class)
    public void failurePathWithAssertUpstream_permittedButMismatched() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();

        final int expectedAssertForUpstream = ALLOWED_ASSERT_UPSTREAM_1;
        callbackResponse.setAssertForUpstream(expectedAssertForUpstream);

        final int expectedStatusCode = ALLOWED_ASSERT_UPSTREAM_2;
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (ApiException ex) {
            // NB: standard API error due to bad assert request

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getMessage()));
            assertThat(exceptionCatalogueResponse.getDetails().get("assertForUpstream"), is(expectedAssertForUpstream));
            assertThat(exceptionCatalogueResponse.getDetails().get("callbacksHttpStatus"), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = ApiException.class)
    public void failurePathWithAssertUpstream_notPermitted() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";
        final CallbackResponse callbackResponse = new CallbackResponse();

        final int notPermittedStatusCode = ASSERT_UPSTREAM_NOT_PERMITTED;
        callbackResponse.setAssertForUpstream(notPermittedStatusCode);

        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(notPermittedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (ApiException ex) {
            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getMessage()));
            assertThat(exceptionCatalogueResponse.getDetails().get("assertForUpstream"), is(notPermittedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void failurePathWithAssertUpstream_autoAssert() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final int expectedStatusCode = AUTO_ASSERT_UPSTREAM_1;
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(ok("non-JSON").withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void failurePathWithAssertUpstream_autoAssert_402SpecialCase() throws Exception {
        final String testUrl = "http://localhost:" + wiremockPort + "/test-callback";

        final int expectedStatusCode = AUTO_ASSERT_UPSTREAM_402;
        stubFor(post(urlMatching("/test-callback.*"))
            .willReturn(ok(null).withStatus(expectedStatusCode)));

        try {
            callbackService.send(testUrl, new CaseEvent(), null, new CaseDetails(), false);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            // verify CatalogueResponse matches 402 special case for payment required: see RDM-6411 & RDM-7224
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_PAYMENT_REQUIRED.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_PAYMENT_REQUIRED.getMessage()));

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
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
        } catch (CallbackException e) {
            // CallbackException >> 504 HttpStatus.GATEWAY_TIMEOUT
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
        final String testWarning1 = "WARNING 1";
        final String testWarning2 = "WARNING 2";

        final CallbackResponse callbackResponse = new CallbackResponse();
        final List<String> warnings = new ArrayList<>();
        warnings.add(testWarning1);
        warnings.add(testWarning2);

        callbackResponse.setWarnings(warnings);

        try {
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, false);
        } catch (ApiException ex) {
            // NB: exception thrown as warning present

            // verify warnings present in exception
            assertTrue(ex.getCallbackWarnings().contains(testWarning1));
            assertTrue(ex.getCallbackWarnings().contains(testWarning2));

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, List<String>>> exceptionCatalogueResponse = (CatalogueResponse<Map<String, List<String>>>)ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            // verify CatalogueResponse contains callback warnings
            Map<String, List<String>> details = exceptionCatalogueResponse.getDetails();
            assertNotNull(details);
            List<String> callbackWarnings = details.get("callbackWarnings");
            assertNotNull(callbackWarnings);
            assertTrue(callbackWarnings.contains(testWarning1));
            assertTrue(callbackWarnings.contains(testWarning2));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithErrorsAndIgnore() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final String testError = "an error";
        final String testWarning = "a warning";
        callbackResponse.setErrors(Collections.singletonList(testError));
        callbackResponse.setWarnings(Collections.singletonList(testWarning));

        try {
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
        } catch (ApiException ex) {
            // NB: exception thrown as errors present and only warnings can be ignored

            // verify errors and warnings present in exception
            assertTrue(ex.getCallbackErrors().contains(testError));
            assertTrue(ex.getCallbackWarnings().contains(testWarning));

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, List<String>>> exceptionCatalogueResponse = (CatalogueResponse<Map<String, List<String>>>)ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            // verify CatalogueResponse contains callback errors and warnings
            Map<String, List<String>> details = exceptionCatalogueResponse.getDetails();
            assertNotNull(details);
            List<String> callbackErrors = details.get("callbackErrors");
            assertNotNull(callbackErrors);
            assertTrue(callbackErrors.contains(testError));
            List<String> callbackWarnings = details.get("callbackWarnings");
            assertNotNull(callbackWarnings);
            assertTrue(callbackWarnings.contains(testWarning));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test
    public void validateCallbackErrorsAndWarningsWithWarningsAndIgnore() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);

        final List<String> warnings = Collections.singletonList("Test");
        callbackResponse.setWarnings(warnings);
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
        // NB: no exception thrown as warnings should been ignored when flag is set
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithCatalogueResponse() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));

        final String expectedCatalogueResponseDetailsCode = "TEST.1001";
        final String expectedCatalogueResponseDetailsMessage = "Testing";
        final CatalogueResponse callbackCatalogueResponse = Mockito.mock(CatalogueResponse.class);
        Mockito.when(callbackCatalogueResponse.getCode()).thenReturn(expectedCatalogueResponseDetailsCode);
        Mockito.when(callbackCatalogueResponse.getMessage()).thenReturn(expectedCatalogueResponseDetailsMessage);

        callbackResponse.setCatalogueResponse(callbackCatalogueResponse);

        try {
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
        } catch (ApiException ex) {
            // NB: exception thrown as errors present and only warnings can be ignored

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));
            // verify CatalogueResponse contains callback's CatalogueResponse
            CatalogueResponse catalogueResponseDetails =
                (CatalogueResponse)((HashMap<String, Object>)exceptionCatalogueResponse.getDetails()).get("callbackCatalogueResponse");
            assertNotNull(catalogueResponseDetails);
            assertThat(catalogueResponseDetails, is(instanceOf(CatalogueResponse.class)));
            assertThat((catalogueResponseDetails).getCode(), is(expectedCatalogueResponseDetailsCode));
            assertThat((catalogueResponseDetails).getMessage(), is(expectedCatalogueResponseDetailsMessage));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = CallbackFailureWithAssertForUpstreamException.class)
    public void validateCallbackErrorsAndWarningsWithErrorsAndAssertForUpstream_permitted() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));

        final int expectedStatusCode = HttpStatus.BAD_REQUEST.value();
        callbackResponse.setAssertForUpstream(expectedStatusCode);

        try {
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
        } catch (CallbackFailureWithAssertForUpstreamException ex) {
            // NB: exception thrown as errors present and only warnings can be ignored

            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_FAILURE.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_FAILURE.getMessage()));

            // verify
            assertThat(ex.getResponseStatusCode(), is(expectedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
    }

    @Test(expected = ApiException.class)
    public void validateCallbackErrorsAndWarningsWithErrorsAndAssertForUpstream_notPermitted() throws Exception {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("an error"));

        final int notPermittedStatusCode = ASSERT_UPSTREAM_NOT_PERMITTED;
        callbackResponse.setAssertForUpstream(notPermittedStatusCode);

        try {
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, true);
        } catch (ApiException ex) {
            // verify CatalogueResponse provided
            CatalogueResponse<Map<String, Object>> exceptionCatalogueResponse = ex.getCatalogueResponse();
            assertNotNull(exceptionCatalogueResponse);
            assertThat(exceptionCatalogueResponse.getCode(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getCode()));
            assertThat(exceptionCatalogueResponse.getMessage(), is(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getMessage()));
            assertThat(exceptionCatalogueResponse.getDetails().get("assertForUpstream"), is(notPermittedStatusCode));

            // rethrow to enable standard junit expected exception verification
            throw ex;
        }
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
        final CallbackService underTest = new CallbackService(Mockito.mock(SecurityUtils.class), restTemplate, applicationParams);
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

    // NB: cannot use mockito.mock as this will not transfer to JSON correctly
    private CatalogueResponse generateCustomCatalogueResponse(String code, String message) throws IOException {
        final String catalogueResponseJson = String.format("{ \"code\": \"%s\", \"message\": \"%s\" }", code, message);
        final ObjectMapper mapper = new ObjectMapper();

        return mapper.readerFor(CatalogueResponse.class).readValue(catalogueResponseJson);
    }
}
