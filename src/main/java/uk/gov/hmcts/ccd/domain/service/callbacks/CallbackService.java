package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.util.Optional;

import static org.springframework.util.CollectionUtils.isEmpty;

// RDM-4316 discarded timeout/backoff value in case event definition until requirements are cleared

@Service
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    @Autowired
    public CallbackService(final SecurityUtils securityUtils,
                           @Qualifier("restTemplate") final RestTemplate restTemplate) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public Optional<CallbackResponse> send(final String url,
                                           final CaseEventDefinition caseEvent,
                                           final CaseDetails caseDetailsBefore,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {

        return sendSingleRequest(url, caseEvent, caseDetailsBefore, caseDetails, ignoreWarning);
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public <T> ResponseEntity<T> send(final String url,
                                      final CaseEventDefinition caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz) {
        return sendSingleRequest(url, caseEvent, caseDetailsBefore, caseDetails, clazz);
    }

    public Optional<CallbackResponse> sendSingleRequest(final String url,
                                                        final CaseEventDefinition caseEvent,
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
                                                   final CaseEventDefinition caseEvent,
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
        } catch (RestClientException e) {
            LOG.warn("Unable to connect to callback service {} because of {} {}", url, e.getClass().getSimpleName(), e.getMessage());
            LOG.debug("", e);  // debug stack trace
            return Optional.empty();
        }
    }

    public void validateCallbackErrorsAndWarnings(final CallbackResponse callbackResponse,
                                                  final Boolean ignoreWarning) {
        if (!isEmpty(callbackResponse.getErrors())
            || (!isEmpty(callbackResponse.getWarnings()) && (ignoreWarning == null || !ignoreWarning))) {
            throw new ApiException("Unable to proceed because there are one or more callback Errors or Warnings")
                .withErrors(callbackResponse.getErrors())
                .withWarnings(callbackResponse.getWarnings());
        }
    }
}
