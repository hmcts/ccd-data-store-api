package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
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

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.CollectionUtils.isEmpty;

@Named
@Singleton
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);

    private final SecurityUtils securityUtils;
    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final List<Integer> defaultRetries;

    @Autowired
    public CallbackService(final SecurityUtils securityUtils,
                           final RestTemplate restTemplate,
                           final ApplicationParams applicationParams) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.defaultRetries = applicationParams.getCallbackRetries();
    }

    public Optional<CallbackResponse> send(final String url,
                                           final List<Integer> callbackRetries,
                                           final CaseEvent caseEvent,
                                           final CaseDetails caseDetails) {
        return send(url, callbackRetries, caseEvent, null, caseDetails);
    }

    public Optional<CallbackResponse> send(final String url,
                                           final List<Integer> callbackRetries,
                                           final CaseEvent caseEvent,
                                           final CaseDetails caseDetailsBefore,
                                           final CaseDetails caseDetails) {

        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }

        final CallbackRequest callbackRequest = new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());
        final List<Integer> retries = CollectionUtils.isEmpty(callbackRetries) ? defaultRetries : callbackRetries;

        for (Integer timeout : retries) {
            final Optional<ResponseEntity<CallbackResponse>> responseEntity = sendRequest(url,
                                                                                          CallbackResponse.class,
                                                                                          callbackRequest,
                                                                                          timeout);
            if (responseEntity.isPresent()) {
                return Optional.of(responseEntity.get().getBody());
            }
        }
        throw new CallbackException("Unsuccessful callback to " + url);
    }

    public <T> ResponseEntity<T> send(final String url,
                                      final List<Integer> callbackRetries,
                                      final CaseEvent caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz) {

        final CallbackRequest callbackRequest = new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());
        final List<Integer> retries = isEmpty(callbackRetries) ? defaultRetries : callbackRetries;

        for (Integer timeout : retries) {
            final Optional<ResponseEntity<T>> requestEntity = sendRequest(url, clazz, callbackRequest, timeout);
            if (requestEntity.isPresent()) {
                return requestEntity.get();
            }
        }
        // Sent so many requests and still got nothing, throw exception here
        throw new CallbackException("Unsuccessful callback to " + url);
    }

    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final Class<T> clazz,
                                                        final CallbackRequest callbackRequest,
                                                        final Integer timeout) {
        try {
            LOG.debug("Trying {} with timeout interval {}", url, timeout);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");

            final HttpHeaders securityHeaders = securityUtils.authorizationHeaders();
            if (null != securityHeaders) {
                securityHeaders.forEach((key, values) -> httpHeaders.put(key, values));
            }

            final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);

            //TODO Disable the following code for now; TO INVESTIAGE WHETHER TIMOUT WORKS, IN RELATION TO OTHERS CALLS
            // and socket issues in Azure
//            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//
//            requestFactory.setConnectionRequestTimeout(secondsToMilliseconds(timeout));
//            requestFactory.setReadTimeout(secondsToMilliseconds(timeout));
//            requestFactory.setConnectTimeout(secondsToMilliseconds(timeout));
//            restTemplate.setRequestFactory(requestFactory);
            return Optional.ofNullable(
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
