package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static wiremock.org.apache.http.entity.ContentType.TEXT_PLAIN;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {"http.client.connection.timeout=1500",
    "http.client.max.total=1",
    "http.client.read.timeout=1500",
    "http.client.seconds.idle.connection=1",
    "http.client.max.client_per_route=2",
    "http.client.validate.after.inactivity=1"})
@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class RestTemplateProviderTest {

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    private static final String RESPONSE_BODY = "Of course la la land";
    private static final String URL = "/ng/itb";
    private static final String MIME_TYPE = TEXT_PLAIN.getMimeType();

    @Autowired
    private RestTemplateProvider restTemplateProvider;

    @Test
    public void restTemplateShouldBeUsable() {

        RestTemplate restTemplate = restTemplateProvider.provide(5);

        assertNotNull(restTemplate);

        stubResponse();

        final RequestEntity<String>
            request =
            new RequestEntity<>(PUT, URI.create("http://localhost:" + wiremockPort + URL));

        final ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertResponse(response);
    }

    @Test(expected = ResourceAccessException.class)
    public void shouldTimeOut() {
        RestTemplate restTemplate = restTemplateProvider.provide(1);
        assertNotNull(restTemplate);
        stubFor(get(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK).withFixedDelay(2000)));

        final RequestEntity<String>
            request =
            new RequestEntity<>(GET, URI.create("http://localhost:" + wiremockPort + URL));

        restTemplate.exchange(request, String.class);
    }

    @Test
    public void shouldBeAbleToUseMultipleTimes() throws Exception {
        stubResponse();

        // rest template 1 will respond immediately
        final RequestEntity<String> request = new RequestEntity<>(PUT, URI.create("http://localhost:" + wiremockPort + URL));
        RestTemplate restTemplate = restTemplateProvider.provide(3);
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertResponse(response);

        stubResponse(2);

        // rest template 2 should fail as read timeout 1 seconds and fixed delay 2 s
        RestTemplate restTemplate2 = restTemplateProvider.provide(1);
        assertThrows(ResourceAccessException.class, () -> restTemplate2.exchange(request, String.class));

        stubResponse(2);

        // rest template 1 should still pass as read timeout 3 s and fixed delay 2 s
        response = restTemplate.exchange(request, String.class);
        assertResponse(response);
    }

    private void stubResponse(int fixedDelayInSeconds) {
        stubFor(put(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK)
            .withHeader(CONTENT_TYPE, MIME_TYPE)
            .withBody(RESPONSE_BODY)
            .withFixedDelay(fixedDelayInSeconds * 1000)));
    }

    private void stubResponse() {
        stubFor(put(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK)
            .withHeader(CONTENT_TYPE, MIME_TYPE)
            .withBody(RESPONSE_BODY)));
    }


    private void assertResponse(final ResponseEntity<String> response) {
        assertThat(response.getBody(), is(RESPONSE_BODY));
        assertThat(response.getHeaders().get(CONTENT_TYPE), contains(MIME_TYPE));
        assertThat(response.getStatusCode().value(), is(SC_OK));
    }
}
