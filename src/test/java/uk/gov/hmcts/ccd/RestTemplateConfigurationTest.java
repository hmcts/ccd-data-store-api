package uk.gov.hmcts.ccd;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static wiremock.com.google.common.collect.Lists.newArrayList;
import static wiremock.org.apache.http.entity.ContentType.APPLICATION_JSON;

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
            new RequestEntity<>(PUT, URI.create("http://localhost:" + wiremockPort + URL));

        final ResponseEntity<JsonNode> response = restTemplate.exchange(request, JsonNode.class);
        assertResponse(response);
    }

    @Test(expected = ResourceAccessException.class)
    public void shouldTimeOut() {
        assertNotNull(restTemplate);
        stubFor(get(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK).withFixedDelay(2000)));

        final RequestEntity<String>
            request =
            new RequestEntity<>(GET, URI.create("http://localhost:" + wiremockPort + URL));

        restTemplate.exchange(request, String.class);
    }

    @Ignore("for local dev only")
    @Test
    public void shouldBeAbleToUseMultipleTimes() throws Exception {
        stubResponse();
        final List<Future<Integer>> futures = newArrayList();
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final int totalNumberOfCalls = 200;

        for (int i = 0; i < totalNumberOfCalls; i++) {
            futures.add(executorService.submit(() -> {
                final RequestEntity<String>
                    request =
                    new RequestEntity<>(PUT, URI.create("http://localhost:" + wiremockPort + URL));
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
