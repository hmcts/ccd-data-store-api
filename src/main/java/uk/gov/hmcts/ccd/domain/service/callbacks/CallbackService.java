package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);
    public static final int CALLBACK_RETRY_INTERVAL_MULTIPLIER = 3;

    private final SecurityUtils securityUtils;
    private final List<Integer> defaultCallbackRetryIntervalsInSeconds;
    private final Integer defaultCallbackTimeoutInMillis;
    private final RestTemplateProvider restTemplateProvider;

    static class CallbackRetryContext {
        private final Integer callbackRetryInterval;
        private final Integer callbackRetryTimeout;

        CallbackRetryContext(final Integer callbackRetryInterval, final Integer callbackRetryTimeout) {
            this.callbackRetryInterval = callbackRetryInterval;
            this.callbackRetryTimeout = callbackRetryTimeout;
        }

        Integer getCallbackRetryInterval() {
            return callbackRetryInterval;
        }

        Integer getCallbackRetryTimeout() {
            return callbackRetryTimeout;
        }

    }

    @Autowired
    public CallbackService(final SecurityUtils securityUtils,
                           final ApplicationParams applicationParams,
                           final RestTemplateProvider restTemplateProvider) {
        this.securityUtils = securityUtils;
        this.defaultCallbackRetryIntervalsInSeconds = applicationParams.getCallbackRetryIntervalsInSeconds();
        this.defaultCallbackTimeoutInMillis = applicationParams.getCallbackReadTimeoutInMillis();
        this.restTemplateProvider = restTemplateProvider;
    }

    public Optional<CallbackResponse> send(final String url,
                                           final List<Integer> callbackRetryTimeouts,
                                           final CaseEvent caseEvent,
                                           final CaseDetails caseDetails) {
        return send(url, callbackRetryTimeouts, caseEvent, null, caseDetails);
    }

    public Optional<CallbackResponse> send(final String url,
                                           final List<Integer> callbackRetryTimeouts,
                                           final CaseEvent caseEvent,
                                           final CaseDetails caseDetailsBefore,
                                           final CaseDetails caseDetails) {

        return Optional.ofNullable(send(url, callbackRetryTimeouts, caseEvent, caseDetailsBefore, caseDetails, CallbackResponse.class, false).getBody());
    }

    public <T> ResponseEntity<T> send(final String url,
                                      final List<Integer> callbackRetryTimeouts,
                                      final CaseEvent caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz,
                                      final boolean ignoreWarning) {

        if (url == null || url.isEmpty()) {
            return noResponse();
        }

        final CallbackRequest callbackRequest = ignoreWarning ?
            new CallbackRequest(caseDetails,
                caseDetailsBefore,
                caseEvent.getId(),
                true) :
            new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());

        List<CallbackRetryContext> retryContextList = buildCallbackRetryContexts(ofNullable(callbackRetryTimeouts).orElse(Lists.newArrayList()));

        for (CallbackRetryContext retryContext : retryContextList) {
            sleep(retryContext.getCallbackRetryInterval());
            final Optional<ResponseEntity<T>> responseEntity = sendRequest(url,
                clazz,
                callbackRequest,
                restTemplateProvider.provide(retryContext.getCallbackRetryTimeout()));
            if (responseEntity.isPresent()) {
                return responseEntity.get();
            }
        }
        // Sent so many requests and still got nothing, throw exception here
        LOG.debug("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(), caseEvent.getId());
        throw new CallbackException("Unsuccessful callback to " + url);
    }

    private <T> ResponseEntity<T> noResponse() {
        return ResponseEntity.ok().build();
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

    private List<CallbackRetryContext> buildCallbackRetryContexts(final List<Integer> callbackRetryTimeouts) {
        List<CallbackRetryContext> retryContextList = Lists.newArrayList();
        if (!callbackRetryTimeouts.isEmpty()) {
            retryContextList.add(new CallbackRetryContext(0, callbackRetryTimeouts.remove(0)));
            if (!callbackRetryTimeouts.isEmpty()) {
                retryContextList.add(new CallbackRetryContext(1, callbackRetryTimeouts.remove(0)));
                for (int i = 0; i < callbackRetryTimeouts.size(); i++) {
                    retryContextList.add(
                        new CallbackRetryContext(
                            getLastElement(retryContextList).getCallbackRetryInterval() * CALLBACK_RETRY_INTERVAL_MULTIPLIER,
                            callbackRetryTimeouts.get(i)));
                }
            }
        } else {
            this.defaultCallbackRetryIntervalsInSeconds.forEach(cbRetryInterval -> retryContextList.add(new CallbackRetryContext(cbRetryInterval, defaultCallbackTimeoutInMillis)));
        }
        return retryContextList;
    }

    private CallbackRetryContext getLastElement(final List<CallbackRetryContext> retryContextList) {
        return retryContextList.get(retryContextList.size() - 1);
    }

    @SuppressWarnings({"squid:S2139", "squid:S00112"})
    private void sleep(final Integer timeout) {
        try {
            TimeUnit.SECONDS.sleep((long) timeout);
        } catch (Exception e) {
            LOG.warn("Error while performing a sleep of timeout={}", timeout, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final Class<T> clazz,
                                                        final CallbackRequest callbackRequest,
                                                        final RestTemplate restTemplate) {
        try {
            LOG.info("Trying {}", url);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            final HttpHeaders securityHeaders = securityUtils.authorizationHeaders();
            if (null != securityHeaders) {
                securityHeaders.forEach((key, values) -> httpHeaders.put(key, values));
            }
            final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);

            return ofNullable(
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, clazz));
        } catch (RestClientException e) {
            LOG.info("Unable to connect to callback service {} because of {} {}",
                url,
                e.getClass().getSimpleName(),
                e.getMessage());
            LOG.debug("", e);  // debug stack trace
            return Optional.empty();
        }
    }

    private int secondsToMilliseconds(final Integer timeout) {
        return (int) TimeUnit.SECONDS.toMillis(timeout);
    }
}
