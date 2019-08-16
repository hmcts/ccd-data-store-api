package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.google.common.collect.Lists;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
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

    private final SecurityUtils securityUtils;
    private final List<Integer> defaultCallbackRetryIntervalsInSeconds;
    private final Integer defaultCallbackTimeoutInMillis;
    private final RestTemplate restTemplate;

    @ToString
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
                           @Qualifier("restTemplate") final RestTemplate restTemplate) {
        this.securityUtils = securityUtils;
        this.defaultCallbackRetryIntervalsInSeconds = applicationParams.getCallbackRetryIntervalsInSeconds();
        this.defaultCallbackTimeoutInMillis = applicationParams.getCallbackReadTimeoutInMillis();
        this.restTemplate = restTemplate;
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
        ResponseEntity<CallbackResponse> responseResponseEntity = send(url, callbackRetryTimeouts, caseEvent,
            caseDetailsBefore, caseDetails, CallbackResponse.class, false);
        return Optional.ofNullable(responseResponseEntity.getBody());
    }

    @SuppressWarnings("javasecurity:S5145")
    public <T> ResponseEntity<T> send(final String url,
                                      final List<Integer> callbackRetryTimeouts,
                                      final CaseEvent caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz,
                                      final boolean ignoreWarning) {
        if (url == null || url.isEmpty()) {
            return ResponseEntity.of(Optional.empty());
        }

        final CallbackRequest callbackRequest = ignoreWarning ?
            new CallbackRequest(caseDetails,
                caseDetailsBefore,
                caseEvent.getId(),
                true) :
            new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());

        List<Integer> retryTimeouts = ofNullable(callbackRetryTimeouts).orElse(Lists.newArrayList());
        List<CallbackRetryContext> retryContextList = buildCallbackRetryContexts(retryTimeouts);
        LOG.info("Built callbackContext={} for caseType={} event={} url={}", retryContextList,
            caseDetails.getCaseTypeId(), caseEvent.getId(), url);

        for (CallbackRetryContext retryContext : retryContextList) {
            sleep(retryContext.getCallbackRetryInterval());
            try {
                final Optional<ResponseEntity<T>> optionalHttpResponse = sendRequest(url, callbackRequest, clazz,
                    caseDetails.getCaseTypeId(), caseEvent.getId());
                return optionalHttpResponse.orElseThrow(() -> {
                        LOG.warn("Unsuccessful callback to url={} for caseType={} and event={} due to no response", url,
                            caseDetails.getCaseTypeId(), caseEvent.getId());
                        return new CallbackException("Unsuccessful callback to " + url);
                    }
                );
            } catch (RestClientException rce) {
                LOG.warn("Unsuccessful callback to url={} for caseType={} and event={} due to exception={}", url,
                    caseDetails.getCaseTypeId(), caseEvent.getId(), rce.toString());
            }
        }
        // Sent so many requests and still got nothing, throw exception here
        LOG.warn("Retry context exhausted. Unsuccessful callback to url={} for caseType={} and event={}", url,
            caseDetails.getCaseTypeId(), caseEvent.getId());
        throw new CallbackException("Unsuccessful callback to url=" + url);
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
        if (isCallbackRetriesDisabled(callbackRetryTimeouts)) {
            retryContextList.add(new CallbackRetryContext(0, callbackRetryTimeouts.remove(0)));
        } else {
            this.defaultCallbackRetryIntervalsInSeconds.forEach(cbRetryInterval -> {
                retryContextList.add(new CallbackRetryContext(cbRetryInterval, defaultCallbackTimeoutInMillis));
            });
        }
        return retryContextList;
    }

    private boolean isCallbackRetriesDisabled(final List<Integer> callbackRetryTimeouts) {
        return callbackRetryTimeouts.size() == 1 && callbackRetryTimeouts.get(0) == 0;
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

    @SuppressWarnings("javasecurity:S5145")
    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final CallbackRequest callbackRequest,
                                                        final Class<T> clazz,
                                                        final String caseTypeId,
                                                        final String eventId) {
        LOG.info("Trying {}", url);

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        final HttpHeaders securityHeaders = securityUtils.authorizationHeaders();
        if (null != securityHeaders) {
            securityHeaders.forEach((key, values) -> httpHeaders.put(key, values));
        }
        final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);

        StopWatch sw = new StopWatch();
        sw.start();
        ResponseEntity<T> exchange = restTemplate.exchange(url, HttpMethod.POST, requestEntity, clazz);
        sw.stop();
        LOG.info("CallbackExecutionTime={} caseType={} event={} url={}", sw.getTotalTimeMillis(), caseTypeId, eventId, url);
        return ofNullable(exchange);

    }

}
