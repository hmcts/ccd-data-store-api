package uk.gov.hmcts.ccd.data.definition;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

@Service
public class DefinitionStoreClient {

    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;

    public DefinitionStoreClient(@Qualifier("definitionStoreRestTemplate") RestTemplate restTemplate,
                                 SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
    }

    private <T> HttpEntity<T> createRequestEntity() {
        return new HttpEntity<>(securityUtils.authorizationHeaders());
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${definition-store.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${definition-store.retry.maxDelay}"))
    public <T> ResponseEntity<T> invokeGetRequest(final String url, Class<T> responseType,
                                                  Map<String, String> queryParams) {
        return restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), responseType, queryParams);
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${definition-store.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${definition-store.retry.maxDelay}"))
    public <T> ResponseEntity<T> invokeGetRequest(final String url, final Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), responseType, Collections.emptyMap());
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${definition-store.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${definition-store.retry.maxDelay}"))
    <T> ResponseEntity<T> invokeGetRequest(final URI url, final Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), responseType);
    }
}
