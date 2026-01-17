package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.core5.util.Timeout;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource(properties = {"http.client.connection.timeout=1500",
    "http.client.max.total=1",
    "http.client.read.timeout=1500",
    "http.client.seconds.idle.connection=1",
    "http.client.max.client_per_route=2",
    "http.client.validate.after.inactivity=1"})
public class RestTemplateConfigurationTest extends WireMockBaseTest {

    @Autowired
    private RestTemplate restTemplate;

    private static final String RESPONSE_BODY = "{ \"test\": \"name\"}";
    private static final String URL = "/ng/itb";
    private static final String MIME_TYPE = APPLICATION_JSON.getMimeType();

    @Test
    public void restTemplateShouldBeUsable() throws Exception {
        assertNotNull(restTemplate);

        stubResponse();

        final RequestEntity<String>
            request =
            new RequestEntity<>(PUT, URI.create(hostUrl + URL));

        final ResponseEntity<JsonNode> response = restTemplate.exchange(request, JsonNode.class);
        assertResponse(response);
    }

    @Test
    public void shouldTimeOut() {
        assertNotNull(restTemplate);
        stubFor(get(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK).withFixedDelay(2000)));

        final RequestEntity<String>
            request =
            new RequestEntity<>(GET, URI.create(hostUrl + URL));

        assertThrows(ResourceAccessException.class, () -> restTemplate.exchange(request, String.class));
    }

    @Disabled("for local dev only")
    @Test
    public void shouldBeAbleToUseMultipleTimes() throws Exception {
        stubResponse();
        final List<Future<Integer>> futures = new ArrayList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final int totalNumberOfCalls = 200;

        for (int i = 0; i < totalNumberOfCalls; i++) {
            futures.add(executorService.submit(() -> {
                final RequestEntity<String>
                    request =
                    new RequestEntity<>(PUT, URI.create(hostUrl + URL));
                final ResponseEntity<JsonNode> response = restTemplate.exchange(request, JsonNode.class);
                assertResponse(response);
                return response.getStatusCode().value();
            }));
        }

        assertThat(futures, hasSize(totalNumberOfCalls));

        for (Future<Integer> future: futures) {
            assertThat(future.get(), is(SC_OK));
        }
    }

    @Test
    public void shouldApplyDistinctConnectAndReadTimeoutsWhenConfiguredSeparately() {
        final int connectTimeout = 500;
        final int readTimeout = 5000;
        RestTemplateConfiguration configuration = new RestTemplateConfiguration();
        // Provide minimal defaults to satisfy the configuration wiring
        ReflectionTestUtils.setField(configuration, "maxTotalHttpClient", 10);
        ReflectionTestUtils.setField(configuration, "maxSecondsIdleConnection", 1);
        ReflectionTestUtils.setField(configuration, "maxClientPerRoute", 2);
        ReflectionTestUtils.setField(configuration, "validateAfterInactivity", 1);
        ReflectionTestUtils.setField(configuration, "connectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "readTimeout", readTimeout);
        ReflectionTestUtils.setField(configuration, "draftsConnectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "draftsCreateConnectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "definitionStoreConnectionTimeout", connectTimeout);

        ReflectionTestUtils.invokeMethod(configuration, "getHttpClient", connectTimeout, readTimeout);
        Object client = ReflectionTestUtils.invokeMethod(configuration, "getHttpClient", connectTimeout, readTimeout);
        RequestConfig config = ((Configurable) client).getConfig();

        assertThat(config.getConnectTimeout(), is(Timeout.ofMilliseconds(connectTimeout)));
        assertThat(config.getResponseTimeout(), is(Timeout.ofMilliseconds(readTimeout)));
        assertThat(config.getConnectionRequestTimeout(), is(Timeout.ofMilliseconds(connectTimeout)));
    }

    @Test
    public void defaultClientFactoryShouldUseReadTimeoutForSocket() {
        final int connectTimeout = 800;
        final int readTimeout = 3200;
        RestTemplateConfiguration configuration = new RestTemplateConfiguration();
        ReflectionTestUtils.setField(configuration, "maxTotalHttpClient", 10);
        ReflectionTestUtils.setField(configuration, "maxSecondsIdleConnection", 1);
        ReflectionTestUtils.setField(configuration, "maxClientPerRoute", 2);
        ReflectionTestUtils.setField(configuration, "validateAfterInactivity", 1);
        ReflectionTestUtils.setField(configuration, "connectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "readTimeout", readTimeout);
        ReflectionTestUtils.setField(configuration, "draftsConnectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "draftsCreateConnectionTimeout", connectTimeout);
        ReflectionTestUtils.setField(configuration, "definitionStoreConnectionTimeout", connectTimeout);

        Object client = ReflectionTestUtils.invokeMethod(configuration, "getHttpClient");
        RequestConfig config = ((Configurable) client).getConfig();

        assertThat(config.getConnectTimeout(), is(Timeout.ofMilliseconds(connectTimeout)));
        assertThat(config.getResponseTimeout(), is(Timeout.ofMilliseconds(readTimeout)));
        assertThat(config.getConnectionRequestTimeout(), is(Timeout.ofMilliseconds(connectTimeout)));
    }

    private void stubResponse() {
        stubFor(put(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK)
            .withHeader(CONTENT_TYPE, MIME_TYPE)
            .withBody(RESPONSE_BODY)));
    }

    private void assertResponse(final ResponseEntity<JsonNode> response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThat(response.getBody(), is(objectMapper.readValue(RESPONSE_BODY, JsonNode.class)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), contains(MIME_TYPE));
        assertThat(response.getStatusCode().value(), is(SC_OK));
    }
}
