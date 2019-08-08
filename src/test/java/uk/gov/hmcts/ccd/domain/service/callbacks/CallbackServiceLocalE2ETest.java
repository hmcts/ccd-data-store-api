package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.v2.internal.resource.UIStartTriggerResource;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static wiremock.com.google.common.collect.Lists.newArrayList;

@Ignore("Only for local development")
public class CallbackServiceLocalE2ETest {

    RestTemplate restTemplate;

    @BeforeEach
    private void setup() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        cm.setMaxTotal(100);
        cm.closeIdleConnections(5, TimeUnit.SECONDS);
        cm.setDefaultMaxPerRoute(20);
        cm.setValidateAfterInactivity(0);
        final RequestConfig
            config =
            RequestConfig.custom()
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .setSocketTimeout(60000)
                .build();

        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .setConnectionManager(cm)
            .setRetryHandler(new DefaultHttpRequestRetryHandler(3, false))
            .build()));
    }

    @Test
    public void shouldFireMany() {

        final int totalNumberOfCalls = 250;
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4cGFzcHFqdDhsamg5N2xtc20zcGlmbXZodiIsInN1YiI6IjIzIiwiaWF0IjoxNTY1MjUzMjAwLCJleHAiOjE1NjUyODIwMDAsImRhdGEiOiJjYXNld29ya2VyLGNhc2V3b3JrZXItYXV0b3Rlc3QxLGNhc2V3b3JrZXItZGl2b3JjZSxjYXNld29ya2VyLGNhc2V3b3JrZXItdGVzdCxjYXNld29ya2VyLWxvYTEsY2FzZXdvcmtlci1hdXRvdGVzdDEtbG9hMSxjYXNld29ya2VyLWRpdm9yY2UtbG9hMSxjYXNld29ya2VyLWxvYTEsY2FzZXdvcmtlci10ZXN0LWxvYTEiLCJ0eXBlIjoiQUNDRVNTIiwiaWQiOiIyMyIsImZvcmVuYW1lIjoiVXNlciIsInN1cm5hbWUiOiJUZXN0IiwiZGVmYXVsdC1zZXJ2aWNlIjoiQ0NEIiwibG9hIjoxLCJkZWZhdWx0LXVybCI6Imh0dHBzOi8vbG9jYWxob3N0OjkwMDAvcG9jL2NjZCIsImdyb3VwIjoiY2FzZXdvcmtlciJ9.BeU3GYOEppgTt5netJnCFAnwMovyvIgLO8a7ASdQF40");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.ACCEPT, "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-case-trigger.v2+json;charset=UTF-8");
        headers.add("experimental", "true");
        HttpEntity requestEntity = new HttpEntity(headers);

        final List<Future<Integer>> futures = newArrayList();
        final ExecutorService executorService = Executors.newFixedThreadPool(25);

        for (int i = 0; i < totalNumberOfCalls; i++) {
            futures.add((Future<Integer>) executorService.submit(() -> {
                System.out.println("Request");
                ResponseEntity<UIStartTriggerResource> response = restTemplate.exchange(
                    "http://localhost:3453/data/internal/case-types/CaseProgression/event-triggers/createCase?ignore-warning=false",
                    HttpMethod.GET,
                    requestEntity,
                    UIStartTriggerResource.class
                );
                System.out.println(response.getBody());
                return response.getStatusCode().value();
            }));
        }

        assertThat(futures, hasSize(totalNumberOfCalls));

        for (Future<Integer> future : futures) {
            try {
                assertThat(future.get(60, TimeUnit.SECONDS), is(SC_OK));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.println("EXCEPTION=" + e);
                fail();
            }
        }
    }
}
