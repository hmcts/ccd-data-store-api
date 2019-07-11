package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final int CALLBACK_RETRY_INTERVAL_MULTIPLIER = 3;

    private final SecurityUtils securityUtils;
    private final List<Integer> defaultCallbackRetryIntervalsInSeconds;
    private final Integer defaultCallbackTimeoutInMillis;
    private final HttpClient httpClient;

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
                           @Qualifier("httpClient") final HttpClient httpClient) {
        this.securityUtils = securityUtils;
        this.defaultCallbackRetryIntervalsInSeconds = applicationParams.getCallbackRetryIntervalsInSeconds();
        this.defaultCallbackTimeoutInMillis = applicationParams.getCallbackReadTimeoutInMillis();
        this.httpClient = httpClient;
        MAPPER.configure(AUTO_CLOSE_SOURCE, true);
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
            final Optional<HttpResponse> maybeHttpResponse = sendRequest(url,
                callbackRequest,
                retryContext.getCallbackRetryTimeout());
            if (maybeHttpResponse.isPresent()) {
                try {
                    return handleResponse(clazz, maybeHttpResponse.get());
                } catch (Exception e) {
                    LOG.warn("Unsuccessful callback to {} for caseType {} and event {} due to {}", url, caseDetails.getCaseTypeId(), caseEvent.getId(), e);
                    throw new CallbackException("Unsuccessful callback to " + url);
                }
            }
        }
        // Sent so many requests and still got nothing, throw exception here
        LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(), caseEvent.getId());
        throw new CallbackException("Unsuccessful callback to " + url);
    }

    private <T> ResponseEntity<T> handleResponse(final Class<T> clazz, final HttpResponse httpResponse) throws IOException {
        ResponseHandler<T> responseHandler = new ResponseHandler<T>(clazz);
        return ResponseEntity.<T>of(Optional.<T>ofNullable(responseHandler.handleResponse(httpResponse)));
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

    private <T> Optional<HttpResponse> sendRequest(final String url,
                                                            final CallbackRequest callbackRequest,
                                                            final Integer timeout) {
        try {
            LOG.info("Trying {} with timeout {}", url, timeout);

            HttpPost httpPost = new HttpPost(url);
            setConfig(timeout, httpPost);
            setEntity(callbackRequest, httpPost);
            setHeaders(httpPost);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                LOG.warn("Unable to connect to callback service {} because of {}",
                    url,
                    httpResponse.getStatusLine().getReasonPhrase());
                return Optional.empty();
            }
            return ofNullable(httpResponse);
        } catch (IOException e) {
            LOG.warn("Unable to connect to callback service {} because of {} {}",
                url,
                e.getClass().getSimpleName(),
                e.getMessage());
            LOG.debug("", e);  // debug stack trace
            return Optional.empty();
        }
    }

    private void setConfig(final Integer timeout, final HttpPost httpPost) {
        RequestConfig requestConfig = buildRequestConfig(timeout);
        httpPost.setConfig(requestConfig);
    }

    private void setEntity(final CallbackRequest callbackRequest, final HttpPost httpPost) throws UnsupportedEncodingException, JsonProcessingException {
        StringEntity callbackRequestEntity = new StringEntity(MAPPER.writeValueAsString(callbackRequest));
        httpPost.setEntity(callbackRequestEntity);
    }

    private void setHeaders(final HttpPost httpPost) {
        httpPost.setHeader("Content-Type", "application/json");
        final HttpHeaders securityHeaders = securityUtils.authorizationHeaders();
        if (null != securityHeaders) {
            securityHeaders.forEach((key, values) ->
                values.forEach(value ->
                    httpPost.setHeader(key, value)
                ));
        }
    }

    private RequestConfig buildRequestConfig(final Integer timeout) {
        return RequestConfig.custom()
            .setSocketTimeout(secondsToMilliseconds(timeout))
            .setConnectTimeout(secondsToMilliseconds(timeout))
            .setConnectionRequestTimeout(secondsToMilliseconds(timeout))
            .build();
    }

    private int secondsToMilliseconds(final Integer timeout) {
        return (int) TimeUnit.SECONDS.toMillis(timeout);
    }

    class ResponseHandler<T> extends AbstractResponseHandler<T> {

        private final Class<T> clazz;

        public ResponseHandler(final Class<T> clazz) {
            this.clazz = clazz;
        }

        /**
         * Returns the entity as a body as a String.
         */
        @Override
        public T handleEntity(final HttpEntity entity) throws IOException {
            String s = EntityUtils.toString(entity);
            return MAPPER.readValue(s, clazz);
        }

        @Override
        public T handleResponse(
            final HttpResponse response) throws HttpResponseException, IOException {
            return super.handleResponse(response);
        }

    }
}
