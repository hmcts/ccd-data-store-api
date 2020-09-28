package uk.gov.hmcts.ccd.integrations;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Ignore
// FIXME : RDM-7635 - has to mock opendId jwks responses with proper Key set (RS256 public / private key).
public class ServiceToServiceIT extends IntegrationTest {

    private static final String SERVICE_TOKEN = "ServiceToken";
    private static final String INVALID_SERVICE_TOKEN = "InvalidServiceToken";
    private static final String CASE_URL =
            "/caseworkers/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldPassServiceAuthorizationWhenValidServiceToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", SERVICE_TOKEN);
        headers.add("Authorization", "Bearer UserAuthToken");
        headers.add("Content-Type", "application/json");

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCodeValue(), not(401));
        assertThat(response.getStatusCodeValue(), not(403));
        verify(getRequestedFor(urlEqualTo("/s2s/details"))
                   .withHeader("Authorization", equalTo("Bearer " + SERVICE_TOKEN)));

    }

    @Test
    public void shouldFailServiceAuthorizationWhenInvalidServiceToken() {

        stubFor(get(urlEqualTo("/s2s/details"))
                      .withHeader("Authorization", equalTo("Bearer " + INVALID_SERVICE_TOKEN))
                      .willReturn(aResponse().withStatus(401)));

        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", INVALID_SERVICE_TOKEN);
        headers.add("Authorization", "Bearer UserAuthToken");
        headers.add("Content-Type", "application/json");

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCodeValue(), is(403));
        verify(getRequestedFor(urlEqualTo("/s2s/details"))
                   .withHeader("Authorization", equalTo("Bearer " + INVALID_SERVICE_TOKEN)));

    }

}
