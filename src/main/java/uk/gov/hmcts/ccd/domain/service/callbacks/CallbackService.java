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
import org.springframework.util.ObjectUtils;
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

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_SCHEME = "http";
    private static final String METADATA_ENDPOINT = "169.254.169.254";
    private static final List<String> SENSITIVE_HEADERS = List.of(
        HttpHeaders.AUTHORIZATION,
        SecurityUtils.SERVICE_AUTHORIZATION,
        "user-id",
        "user-roles"
    );
    private static final String DEFAULT_CALLBACK_ERROR_MESSAGE
        = "Unable to proceed because there are one or more callback Errors or Warnings";

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
            final String safeUrl = sanitizeUrl(url);
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", safeUrl, caseDetails.getCaseTypeId(),
                caseEvent.getId());
            String callbackTypeString = callbackType != null ? callbackType.getValue() : "null";
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName()
                + " url " + safeUrl + " caseTypeId " + caseDetails.getCaseTypeId() + " caseEvent Id "
                + caseEvent.getId()
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
            LOG.warn("Unsuccessful callback to {} for caseType {} and event {}", sanitizeUrl(url),
                caseDetails.getCaseTypeId(),
                caseEvent.getId());
            return new CallbackException("Callback to service has been unsuccessful for event " + caseEvent.getName());
        });
    }

    private <T> Optional<ResponseEntity<T>> sendRequest(final String url,
                                                        final CallbackType callbackType,
                                                        final Class<T> clazz,
                                                        final CallbackRequest callbackRequest) {
        validateCallbackUrl(url);
        final String safeUrl = sanitizeUrl(url);

        CallbackTelemetryThreadContext.setTelemetryContext(new CallbackTelemetryContext(callbackType));
        int httpStatus = 0;
        Instant startTime = Instant.now();

        try {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            httpHeaders.add(SecurityUtils.SERVICE_AUTHORIZATION, securityUtils.getServiceAuthorization());
            addPassThroughHeaders(httpHeaders);
            final HttpEntity requestEntity = new HttpEntity(callbackRequest, httpHeaders);
            if (logCallbackDetails(url)) {
                LOG.info("Invoking callback {} of type {} with request: {}", safeUrl, callbackType,
                    printCallbackDetails(requestEntity));
            }
            ResponseEntity<T> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, clazz);
            if (logCallbackDetails(url)) {
                LOG.info("Callback {} response received: {}", safeUrl, printCallbackDetails(responseEntity));
            }

            storePassThroughHeadersAsRequestAttributes(responseEntity, requestEntity, request);
            responseEntity = replaceResponseEntityWithUpdatedHeaders(responseEntity, CLIENT_CONTEXT);
            httpStatus = responseEntity.getStatusCode().value();
            return Optional.of(responseEntity);
        } catch (RestClientException e) {
            LOG.warn("Unable to connect to callback service {} because of {} {}",
                safeUrl, e.getClass().getSimpleName(), e.getMessage());
            LOG.debug("", e);  // debug stack trace
            if (e instanceof HttpStatusCodeException) {
                httpStatus = ((HttpStatusCodeException) e).getStatusCode().value();
            }
            return Optional.empty();
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            appinsights.trackCallbackEvent(callbackType, safeUrl, String.valueOf(httpStatus), duration);
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

        if (!ObjectUtils.isEmpty(callbackResponse.getErrorMessageOverride())
            || !isEmpty(callbackResponse.getErrors())
            || (!isEmpty(callbackResponse.getWarnings()) && (ignoreWarning == null || !ignoreWarning))) {

            String errorMessage = Optional.ofNullable(callbackResponse.getErrorMessageOverride())
                .orElse(DEFAULT_CALLBACK_ERROR_MESSAGE);

            throw new ApiException(errorMessage)
                .withErrors(callbackResponse.getErrors())
                .withWarnings(callbackResponse.getWarnings());
        }
    }

    protected void addPassThroughHeaders(final HttpHeaders httpHeaders) {
        if (null != request && null != applicationParams
            && null != applicationParams.getCallbackPassthruHeaderContexts()) {
            applicationParams.getCallbackPassthruHeaderContexts().stream()
                .filter(this::isAllowedPassThroughHeader)
                .forEach(context -> addPassThruContextValuesToHttpHeaders(httpHeaders, context));
        }
    }

    private boolean isAllowedPassThroughHeader(String headerName) {
        return StringUtils.hasLength(headerName)
            && SENSITIVE_HEADERS.stream().noneMatch(sensitive -> sensitive.equalsIgnoreCase(headerName));
    }

    private void validateCallbackUrl(String url) {
        final URI callbackUri;
        try {
            callbackUri = new URI(url);
        } catch (URISyntaxException e) {
            throw new CallbackException("Invalid callback URL: " + sanitizeUrl(url));
        }

        final String scheme = Optional.ofNullable(callbackUri.getScheme()).orElse("").toLowerCase(Locale.UK);
        final String host = callbackUri.getHost();
        if (!StringUtils.hasLength(host)) {
            throw new CallbackException("Callback URL must include a host: " + sanitizeUrl(url));
        }
        if (StringUtils.hasLength(callbackUri.getUserInfo())) {
            throw new CallbackException("Callback URL must not include credentials: " + sanitizeUrl(url));
        }
        if (!isAllowedHost(host, applicationParams.getCallbackAllowedHosts())) {
            throw new CallbackException("Callback URL host is not allowlisted: " + host);
        }
        if (!HTTPS_SCHEME.equals(scheme)
            && !(HTTP_SCHEME.equals(scheme) && isAllowedHost(host, applicationParams.getCallbackAllowedHttpHosts()))) {
            throw new CallbackException("Callback URL scheme is not permitted: " + scheme);
        }
        if (resolvesToPrivateAddress(host) && !isAllowedHost(host, applicationParams.getCallbackAllowPrivateHosts())) {
            throw new CallbackException("Callback URL resolves to a private or local network address: " + host);
        }
    }

    private boolean isAllowedHost(String host, List<String> allowedHosts) {
        if (!StringUtils.hasLength(host) || allowedHosts == null) {
            return false;
        }
        return allowedHosts.stream()
            .filter(StringUtils::hasLength)
            .map(String::trim)
            .anyMatch(allowed -> hostMatches(host, allowed));
    }

    private boolean hostMatches(String host, String allowedHost) {
        if (WILDCARD.equals(allowedHost)) {
            return true;
        }
        final String normalisedHost = host.toLowerCase(Locale.UK);
        final String normalisedAllowedHost = allowedHost.toLowerCase(Locale.UK);
        if (normalisedAllowedHost.startsWith("*.")) {
            String suffix = normalisedAllowedHost.substring(1);
            return normalisedHost.endsWith(suffix);
        }
        return normalisedHost.equals(normalisedAllowedHost);
    }

    private boolean resolvesToPrivateAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivateOrLocal(address)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new CallbackException("Unable to resolve callback host: " + host);
        }
    }

    private boolean isPrivateOrLocal(InetAddress address) {
        if (address instanceof Inet6Address) {
            final byte[] addressBytes = address.getAddress();
            if (addressBytes.length > 0 && (addressBytes[0] & (byte) 0xFE) == (byte) 0xFC) {
                return true; // IPv6 unique local addresses fc00::/7, including fd00::/8
            }
        }
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || METADATA_ENDPOINT.equals(address.getHostAddress());
    }

    private String sanitizeUrl(String url) {
        if (!StringUtils.hasLength(url)) {
            return "<empty>";
        }
        try {
            URI uri = new URI(url);
            String scheme = Optional.ofNullable(uri.getScheme()).orElse("unknown");
            String host = Optional.ofNullable(uri.getHost()).orElse("unknown-host");
            int port = uri.getPort();
            String path = Optional.ofNullable(uri.getPath()).orElse("");
            String portPart = port > -1 ? ":" + port : "";
            return scheme + "://" + host + portPart + path;
        } catch (URISyntaxException e) {
            return "<invalid-url>";
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
