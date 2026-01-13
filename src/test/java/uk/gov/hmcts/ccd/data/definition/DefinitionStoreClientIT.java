package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.net.URI;
import java.net.SocketTimeoutException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@EnableRetry
@DirtiesContext
public class DefinitionStoreClientIT extends WireMockBaseTest {

    @MockitoBean(name = "definitionStoreRestTemplate")
    private RestTemplate restTemplate;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Autowired
    private DefinitionStoreClient definitionStoreClient;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(definitionStoreClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(definitionStoreClient, "securityUtils", securityUtils);
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
    }

    @Test
    public void testShouldInvokeGetRequestWithQueryParamsRetryAfterHttpServerErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Object>>any(), anyMap()))
            .thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> definitionStoreClient.invokeGetRequest("http://localhost",
            Class.class, Collections.emptyMap()));

        verify(restTemplate, times(5)).exchange(
            "http://localhost",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class,
            Collections.emptyMap()
        );
    }

    @Test
    public void testShouldInvokeGetRequestRetryAfterHttpServerErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Object>>any(), anyMap()))
            .thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> definitionStoreClient.invokeGetRequest("http://localhost",
            Class.class));

        verify(restTemplate, times(5)).exchange(
            "http://localhost",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class,
            Collections.emptyMap()
        );
    }

    @Test
    public void testShouldInvokeGetRequestWithURIRetryAfterHttpServerErrorException() {
        when(restTemplate.exchange(any(URI.class), any(), any(), ArgumentMatchers.<Class<Object>>any()))
            .thenThrow(HttpServerErrorException.class);

        URI url = URI.create("http://localhost");
        assertThrows(HttpServerErrorException.class, () -> definitionStoreClient.invokeGetRequest(url, Class.class));

        verify(restTemplate, times(5)).exchange(
            url,
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class
        );
    }

    @Test
    public void testShouldInvokeGetRequestWithQueryParamsNotRetryAfterHttpClientErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Object>>any(), anyMap()))
            .thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> definitionStoreClient.invokeGetRequest("http://localhost",
            Class.class, Collections.emptyMap()));

        verify(restTemplate, times(1)).exchange(
            "http://localhost",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class,
            Collections.emptyMap()
        );
    }

    @Test
    public void testShouldInvokeGetRequestNotRetryAfterHttpClientErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Object>>any(), anyMap()))
            .thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> definitionStoreClient.invokeGetRequest("http://localhost",
            Class.class));

        verify(restTemplate, times(1)).exchange(
            "http://localhost",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class,
            Collections.emptyMap()
        );
    }

    @Test
    public void testShouldInvokeGetRequestWithURINotRetryAfterHttpClientErrorException() {
        when(restTemplate.exchange(any(URI.class), any(), any(), ArgumentMatchers.<Class<Object>>any()))
            .thenThrow(HttpClientErrorException.class);

        URI url = URI.create("http://localhost");
        assertThrows(HttpClientErrorException.class, () -> definitionStoreClient.invokeGetRequest(url, Class.class));

        verify(restTemplate, times(1)).exchange(
            url,
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class
        );
    }

    @Test
    public void testShouldRetryOnSocketReadTimeoutException() throws SocketTimeoutException {
        doAnswer(invocation -> {
            throw new SocketTimeoutException("read timed out");
        })
            .when(restTemplate)
            .exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Object>>any(), anyMap());

        Exception thrown = assertThrows(Exception.class, () -> definitionStoreClient.invokeGetRequest(
            "http://localhost", Class.class, Collections.emptyMap()));
        assertTrue(thrown.getCause() instanceof SocketTimeoutException);

        verify(restTemplate, times(5)).exchange(
            "http://localhost",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Class.class,
            Collections.emptyMap()
        );
    }
}
