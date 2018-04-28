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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.PUT;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestTemplateConfigurationTest {

    @Rule
    public WireMockRule wireMockServer = new WireMockRule(wireMockConfig().dynamicPort());

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void restTemplateShouldBeUsable() {
        assertNotNull(restTemplate);

        final String body = "Of course la la land";
        final String contentType = "Content-Type";
        final String textPlain = "text/plain";
        final int statusCode = 200;
        final String url = "/ng/itb";

        stubFor(put(urlEqualTo(url)).willReturn(aResponse().withStatus(statusCode)
                                                                 .withHeader(contentType, textPlain)
                                                                 .withBody(body)));
        RequestEntity<String>
            request =
            new RequestEntity<>("things", PUT, URI.create("http://localhost:" + wireMockServer.port() + url));

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getBody(), is(body));
        assertThat(response.getHeaders().get(contentType), contains(textPlain));
        assertThat(response.getStatusCode().value(), is(statusCode));
    }
}
