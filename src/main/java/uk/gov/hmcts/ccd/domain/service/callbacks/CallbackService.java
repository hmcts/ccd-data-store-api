package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponse;
import uk.gov.hmcts.ccd.domain.model.common.CatalogueResponseElement;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackFailureWithAssertForUpstreamException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.CollectionUtils.isEmpty;

// RDM-4316 discarded timeout/backoff value in case event definition until requirements are cleared

@Service
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;

    @Autowired
    public CallbackService(final SecurityUtils securityUtils,
                           @Qualifier("restTemplate") final RestTemplate restTemplate,
                           final ApplicationParams applicationParams) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public Optional<CallbackResponse> send(final String url,
                                           final CaseEvent caseEvent,
                                           final CaseDetails caseDetailsBefore,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {

        return sendSingleRequest(url, caseEvent, caseDetailsBefore, caseDetails, ignoreWarning);
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public <T> ResponseEntity<T> send(final String url,
                                      final CaseEvent caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz) {
        return sendSingleRequest(url, caseEvent, caseDetailsBefore, caseDetails, clazz);
    }

    public Optional<CallbackResponse> sendSingleRequest(final String url,
                                                        final CaseEvent caseEvent,
                                                        final CaseDetails caseDetailsBefore,
                                                        final CaseDetails caseDetails,
                                                        final Boolean ignoreWarning) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }
        final CallbackRequest callbackRequest = new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId(), ignoreWarning);
        final Optional<ResponseEntity<CallbackResponse>> responseEntity = sendRequest(url, CallbackResponse.class, callbackRequest);
        return responseEntity.map(re -> Optional.of(re.getBody())).orElseThrow(() -> {
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(), caseEvent.getId());
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName());
        });
    }

    public <T> ResponseEntity<T> sendSingleRequest(final String url,
                                               final CaseEvent caseEvent,
                                               final CaseDetails caseDetailsBefore,
                                               final CaseDetails caseDetails,
                                               final Class<T> clazz) {
        final CallbackRequest callbackRequest = new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());
        final Optional<ResponseEntity<T>> requestEntity = sendRequest(url, clazz, callbackRequest);
        return requestEntity.orElseThrow(() -> {
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(), caseEvent.getId());
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName());
        });
    }

    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final Class<T> clazz,
                                                        final CallbackRequest callbackRequest) {
        try {
            LOG.debug("Invoking callback {}", url);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            final HttpHeaders securityHeaders = securityUtils.authorizationHeaders();
            if (null != securityHeaders) {
                securityHeaders.forEach((key, values) -> httpHeaders.put(key, values));
            }
            final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);
            return Optional.ofNullable(restTemplate.exchange(url, HttpMethod.POST, requestEntity, clazz));

        } catch (RestClientResponseException e) {

            // intercept callback response
            if (clazz == CallbackResponse.class) {
                handleInterceptedCallbackFailure(e);
            }

            // continue with standard 'Unable to connect' processing
            return unableToConnectExceptionProcessing(url, e);

        } catch (RestClientException e) {
            return unableToConnectExceptionProcessing(url, e);
        }
    }

    private void handleInterceptedCallbackFailure(RestClientResponseException e) {
        // extract values from exception
        final int rawStatusCode = e.getRawStatusCode();
        final CallbackResponse callbackResponse = extractCallbackResponseFromCallbackResponseBody(e.getResponseBodyAsString());

        if (checkAssertForUpstreamRequested(callbackResponse, rawStatusCode)) {
            final CatalogueResponse callbackCatalogueResponse = (callbackResponse != null ? callbackResponse.getCatalogueResponse() : null);
            final CatalogueResponseElement catalogueResponseElement = (rawStatusCode == HttpStatus.PAYMENT_REQUIRED.value()
                    ? CatalogueResponseElement.CALLBACK_PAYMENT_REQUIRED // special case for 402
                    : CatalogueResponseElement.CALLBACK_FAILURE); // default

            final CatalogueResponse catalogueResponse = generateCCDCatalogueResponseFromCallbackResponse(catalogueResponseElement, callbackResponse);

            // if callback's CatalogueResponse contained a message raise exception using it
            if (callbackCatalogueResponse != null && StringUtils.isNotBlank(callbackCatalogueResponse.getMessage())) {
                throw new CallbackFailureWithAssertForUpstreamException(catalogueResponse, rawStatusCode, callbackCatalogueResponse.getMessage());
            }

            throw new CallbackFailureWithAssertForUpstreamException(catalogueResponse, rawStatusCode);
        }
    }

    /**
     * Checks if callback's status code and message should be asserted upstream.
     *
     * @param callbackResponse Populated callback response object
     * @param callbacksHttpStatus Received callback HttpStatus code
     *
     * @return true if this is a AssertForUpstream request that is valid
     *
     * @throws ApiException if AssertForUpstream request has been made but is invalid
     */
    private boolean checkAssertForUpstreamRequested(CallbackResponse callbackResponse, int callbacksHttpStatus) {

        // priority given to AutoAssertUpstreamList
        if (applicationParams.getCallbackStatusAutoAssertUpstreamList().contains(callbacksHttpStatus)) {
            return true; // callback's HTTP Status configured for automatic assert
        }

        // otherwise check any AssertForUpstream value in the callback response
        final Integer assertForUpstreamValue = callbackResponse != null ? callbackResponse.getAssertForUpstream() : null;
        if (assertForUpstreamValue != null) {

            // if not on permitted list throw error
            if (!applicationParams.getCallbackStatusAllowedAssertUpstreamList().contains(assertForUpstreamValue)) {
                Map<String, Object> catalogueResponseDetails = new HashMap<>();
                catalogueResponseDetails.put("assertForUpstream", assertForUpstreamValue);

                throw new ApiException(
                    new CatalogueResponse(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM, catalogueResponseDetails),
                    String.format("%s: HttpStatus not permitted",
                        CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getMessage())
                );
            }

            // if mismatch between callback's HttpStatus and requested assert value throw error
            if (callbacksHttpStatus != assertForUpstreamValue.intValue()) {

                Map<String, Object> catalogueResponseDetails = new HashMap<>();
                catalogueResponseDetails.put("assertForUpstream", assertForUpstreamValue);
                catalogueResponseDetails.put("callbacksHttpStatus", callbacksHttpStatus);

                throw new ApiException(
                    new CatalogueResponse(CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM, catalogueResponseDetails),
                    String.format("%s: Callback's HttpStatus does not match requested value",
                        CatalogueResponseElement.CALLBACK_BAD_ASSERT_FOR_UPSTREAM.getMessage())
                );
            }

            return true; // otherwise specified AssertForUpstream value must be OK
        }

        return false; // not an AssertForUpstream request
    }

    private Optional unableToConnectExceptionProcessing(String url, RestClientException e) {
        LOG.warn("Unable to connect to callback service {} because of {} {}", url, e.getClass().getSimpleName(), e.getMessage());
        LOG.debug("", e);  // debug stack trace
        return Optional.empty();
    }

    public void validateCallbackErrorsAndWarnings(final CallbackResponse callbackResponse,
                                                  final Boolean ignoreWarning) {
        if (!isEmpty(callbackResponse.getErrors())
            || (!isEmpty(callbackResponse.getWarnings()) && (ignoreWarning == null || !ignoreWarning))) {

            CatalogueResponse catalogueResponse =
                generateCCDCatalogueResponseFromCallbackResponse(CatalogueResponseElement.CALLBACK_FAILURE, callbackResponse);

            throw new ApiException(catalogueResponse, "Unable to proceed because there are one or more callback Errors or Warnings")
                .withErrors(callbackResponse.getErrors())
                .withWarnings(callbackResponse.getWarnings());
        }
    }

    private CallbackResponse extractCallbackResponseFromCallbackResponseBody(String responseBody) {
        if (responseBody != null) {
            ObjectMapper mapper = new ObjectMapper();

            try {
                return mapper.readerFor(CallbackResponse.class).readValue(responseBody);

            } catch (IOException ex) {
                LOG.warn("Unable to parse callback response", ex);
            }
        }
        return null;
    }

    private CatalogueResponse generateCCDCatalogueResponseFromCallbackResponse(
        CatalogueResponseElement catalogueResponseElement,
        CallbackResponse callbackResponse
    ) {

        Map<String, Object> catalogueResponseDetails = new HashMap<>();

        if (callbackResponse != null) {
            // populate catalogueResponse.details
            if (callbackResponse.getCatalogueResponse() != null) {
                // populate details with callback's CatalogueResponse
                catalogueResponseDetails.put("callbackCatalogueResponse", callbackResponse.getCatalogueResponse());
            } else {
                // populate details from errors and/or warnings
                if (!isEmpty(callbackResponse.getErrors())) {
                    catalogueResponseDetails.put("callbackErrors", callbackResponse.getErrors());
                }
                if (!isEmpty(callbackResponse.getWarnings())) {
                    catalogueResponseDetails.put("callbackWarnings", callbackResponse.getWarnings());
                }
            }
        }

        // clear details if final size is zero
        if (catalogueResponseDetails.size() == 0) {
            catalogueResponseDetails = null;
        }

        return new CatalogueResponse(catalogueResponseElement, catalogueResponseDetails);
    }
}
