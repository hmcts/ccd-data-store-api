package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryContext;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryThreadContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static org.springframework.util.CollectionUtils.isEmpty;

// RDM-4316 discarded timeout/backoff value in case event definition until requirements are cleared

@Service
public class CallbackService {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackService.class);
    private static final String WILDCARD = "*";
    public static final String CLIENT_CONTEXT = "Client-Context";

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final AppInsights appinsights;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    @Autowired
    public CallbackService(final SecurityUtils securityUtils,
                           @Qualifier("restTemplate") final RestTemplate restTemplate,
                           final ApplicationParams applicationParams,
                           AppInsights appinsights,
                           HttpServletRequest request,
                           @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.appinsights = appinsights;
        this.request = request;
        this.objectMapper = objectMapper;
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public Optional<CallbackResponse> send(final String url,
                                           final CallbackType callbackType,
                                           final CaseEventDefinition caseEvent,
                                           final CaseDetails caseDetailsBefore,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {

        return sendSingleRequest(url, callbackType, caseEvent, caseDetailsBefore, caseDetails, ignoreWarning);
    }

    // The retry will be on seconds T=1 and T=3 if the initial call fails at T=0
    @Retryable(value = {CallbackException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 3))
    public <T> ResponseEntity<T> send(final String url,
                                      final CallbackType callbackType,
                                      final CaseEventDefinition caseEvent,
                                      final CaseDetails caseDetailsBefore,
                                      final CaseDetails caseDetails,
                                      final Class<T> clazz) {
        return sendSingleRequest(url, callbackType, caseEvent, caseDetailsBefore, caseDetails, clazz);
    }

    public Optional<CallbackResponse> sendSingleRequest(final String url,
                                                        final CallbackType callbackType,
                                                        final CaseEventDefinition caseEvent,
                                                        final CaseDetails caseDetailsBefore,
                                                        final CaseDetails caseDetails,
                                                        final Boolean ignoreWarning) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }
        final CallbackRequest callbackRequest =
            new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId(), ignoreWarning);
        final Optional<ResponseEntity<CallbackResponse>> responseEntity =
            sendRequest(url, callbackType, CallbackResponse.class, callbackRequest);
        return responseEntity.map(re -> Optional.of(re.getBody())).orElseThrow(() -> {
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(),
                caseEvent.getId());
            String callbackTypeString = callbackType != null ? callbackType.getValue() : "null";
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName()
                + " url " + url + " caseTypeId " + caseDetails.getCaseTypeId() + " caseEvent Id " + caseEvent.getId()
                + " callbackType " + callbackTypeString);
        });
    }

    public <T> ResponseEntity<T> sendSingleRequest(final String url,
                                                   final CallbackType callbackType,
                                                   final CaseEventDefinition caseEvent,
                                                   final CaseDetails caseDetailsBefore,
                                                   final CaseDetails caseDetails,
                                                   final Class<T> clazz) {
        final CallbackRequest callbackRequest = new CallbackRequest(caseDetails, caseDetailsBefore, caseEvent.getId());
        final Optional<ResponseEntity<T>> requestEntity = sendRequest(url, callbackType, clazz, callbackRequest);
        return requestEntity.orElseThrow(() -> {
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", url, caseDetails.getCaseTypeId(),
                caseEvent.getId());
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName());
        });
    }

    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final CallbackType callbackType,
                                                        final Class<T> clazz,
                                                        final CallbackRequest callbackRequest) {

        HttpHeaders securityHeaders = securityUtils.authorizationHeaders();

        CallbackTelemetryThreadContext.setTelemetryContext(new CallbackTelemetryContext(callbackType));
        int httpStatus = 0;
        Instant startTime = Instant.now();

        try {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            addPassThroughHeaders(httpHeaders);
            if (null != securityHeaders) {
                httpHeaders.putAll(securityHeaders);
            }
            final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);
            if (logCallbackDetails(url)) {
                LOG.info("Invoking callback {} of type {} with request: {}", url, callbackType,
                    printCallbackDetails(requestEntity));
            }
            ResponseEntity<T> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, clazz);
            if (logCallbackDetails(url)) {
                LOG.info("Callback {} response received: {}", url, printCallbackDetails(responseEntity));
            }

            storePassThroughHeadersAsRequestAttributes(responseEntity, requestEntity, request);
            responseEntity = replaceResponseEntityWithUpdatedHeaders(responseEntity, CLIENT_CONTEXT);
            httpStatus = responseEntity.getStatusCodeValue();
            return Optional.of(responseEntity);
        } catch (RestClientException e) {
            LOG.warn("Unable to connect to callback service {} because of {} {}",
                url, e.getClass().getSimpleName(), e.getMessage());
            LOG.debug("", e);  // debug stack trace
            if (e instanceof HttpStatusCodeException) {
                httpStatus = ((HttpStatusCodeException) e).getRawStatusCode();
            }
            return Optional.empty();
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            appinsights.trackCallbackEvent(callbackType, url, String.valueOf(httpStatus), duration);
        }
    }

    private String printCallbackDetails(HttpEntity<?> callbackHttpEntity) {
        try {
            return objectMapper.writeValueAsString(callbackHttpEntity);
        } catch (Exception ex) {
            LOG.warn("Unexpected error while logging callback: {}", ex.getMessage());
        }

        return null;
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

    protected void addPassThroughHeaders(final HttpHeaders httpHeaders) {
        if (null != request && null != applicationParams
            && null != applicationParams.getCallbackPassthruHeaderContexts()) {
            applicationParams.getCallbackPassthruHeaderContexts().stream()
                .forEach(context -> addPassThruContextValuesToHttpHeaders(httpHeaders, context));
        }
    }

    private void addPassThruContextValuesToHttpHeaders(HttpHeaders httpHeaders, String context) {
        if (null != request.getAttribute(context)) {
            if (httpHeaders.containsKey(context)) {
                httpHeaders.remove(context);
            }

            httpHeaders.add(context, request.getAttribute(context).toString());
            request.removeAttribute(context);
        } else if (null != request.getHeader(context)) {
            httpHeaders.add(context, request.getHeader(context));
        }
    }

    private void storePassThroughHeadersAsRequestAttributes(ResponseEntity responseEntity,
                                                            HttpEntity requestEntity,
                                                            HttpServletRequest request) {
        HttpHeaders httpHeaders = responseEntity.getHeaders();
        if (null != request && null != applicationParams
            && null != applicationParams.getCallbackPassthruHeaderContexts()) {
            applicationParams.getCallbackPassthruHeaderContexts().stream()
                .filter(context -> StringUtils.hasLength(context) && null != httpHeaders.get(context))
                .forEach(context -> {
                    String headerValue = ClientContextUtil.removeEnclosingSquareBrackets(
                        httpHeaders.get(context).get(0));

                    if (CLIENT_CONTEXT.equalsIgnoreCase(context)) {
                        headerValue = ClientContextUtil.mergeClientContexts(
                            requestEntity.getHeaders().getFirst(context), headerValue);
                    }

                    request.setAttribute(context, headerValue);
                });
        }
    }

    private ResponseEntity replaceResponseEntityWithUpdatedHeaders(final ResponseEntity responseEntity,
                                                                   final String headerName) {
        HttpHeaders headers = responseEntity.getHeaders();
        if (headers != null && headers.get(headerName) != null) {
            HttpHeaders newHeaders = ClientContextUtil.replaceHeader(headers, CLIENT_CONTEXT,
                request.getAttribute(CLIENT_CONTEXT).toString());
            return new ResponseEntity<>(responseEntity.getBody(), newHeaders, responseEntity.getStatusCode());
        } else {
            return responseEntity;
        }
    }

    private boolean logCallbackDetails(final String url) {
        return (!applicationParams.getCcdCallbackLogControl().isEmpty()
            && (WILDCARD.equals(applicationParams.getCcdCallbackLogControl().getFirst())
            || applicationParams.getCcdCallbackLogControl().stream()
            .filter(Objects::nonNull).filter(Predicate.not(String::isEmpty)).anyMatch(url::contains)));
    }
}
