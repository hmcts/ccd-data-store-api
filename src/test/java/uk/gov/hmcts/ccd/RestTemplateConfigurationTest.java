package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static wiremock.org.apache.http.entity.ContentType.TEXT_PLAIN;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {"http.client.connection.timeout=1500"})
public class RestTemplateConfigurationTest {

    @Rule
    public WireMockRule wireMockServer = new WireMockRule(wireMockConfig().dynamicPort());

    @Autowired
    private RestTemplate restTemplate;

    private static final String RESPONSE_BODY = "Of course la la land";
    private static final String URL = "/ng/itb";
    private static final String MIME_TYPE = TEXT_PLAIN.getMimeType();

    @Test public void restTemplateShouldBeUsable() {
        assertNotNull(restTemplate);

        stubFor(put(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK)
                                                           .withHeader(CONTENT_TYPE, MIME_TYPE)
                                                           .withBody(RESPONSE_BODY)));
        RequestEntity<String>
            request =
            new RequestEntity<>(PUT, URI.create("http://localhost:" + wireMockServer.port() + URL));

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getBody(), is(RESPONSE_BODY));
        assertThat(response.getHeaders().get(CONTENT_TYPE), contains(MIME_TYPE));
        assertThat(response.getStatusCode().value(), is(SC_OK));
    }

    @Test(expected = ResourceAccessException.class) public void shouldTimeOut() {
        assertNotNull(restTemplate);
        stubFor(get(urlEqualTo(URL)).willReturn(aResponse().withStatus(SC_OK).withFixedDelay(2000)));

        RequestEntity<String>
            request =
            new RequestEntity<>(GET, URI.create("http://localhost:" + wireMockServer.port() + URL));

        restTemplate.exchange(request, String.class);
    }
}
