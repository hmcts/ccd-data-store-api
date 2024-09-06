package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest
@EnableRetry
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class DefinitionStoreClientIT {

    @MockBean(name = "definitionStoreRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private SecurityUtils securityUtils;

    @Autowired
    private DefinitionStoreClient definitionStoreClient;

    @Test
    public void testShouldInvokeGetRequestWithQueryParamsRetryAfterHttpServerErrorException() {
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
        when(securityUtils.authorizationHeaders()).thenReturn(HttpHeaders.EMPTY);
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
}
