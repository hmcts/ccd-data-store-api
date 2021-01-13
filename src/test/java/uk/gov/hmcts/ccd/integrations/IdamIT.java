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
public class IdamIT extends IntegrationTest {

    private static final String CASE_URL =
        "/caseworkers/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_CITIZEN =
        "/citizens/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_WRONG_ID =
        "/caseworkers/456/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_WRONG_ROLE =
        "/caseworkers/123/jurisdictions/PROBATE/case-types/TestAddressBook/cases/1234123412341238";

    private static final String VALID_IDAM_TOKEN = "Bearer UserAuthToken";
    private static final String VALID_CITIZEN_TOKEN = "Bearer CitizenToken";
    private static final String VALID_LETTERHOLDER_TOKEN = "Bearer LetterHolderToken";
    private static final String INVALID_IDAM_TOKEN = "Bearer InvalidUserAuthToken";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldPassUserAuthorizationWhenValidCaseworker() {

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL,
            HttpMethod.GET,
            new HttpEntity<>(validHeaders()),
            String.class
        );

        assertThat(response.getStatusCodeValue(), not(401));
        assertThat(response.getStatusCodeValue(), not(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(VALID_IDAM_TOKEN)));
    }

    @Test
    public void shouldPassUserAuthorizationWhenValidCitizen() {

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL_CITIZEN,
            HttpMethod.GET,
            new HttpEntity<>(validHeaders(VALID_CITIZEN_TOKEN)),
            String.class
        );

        assertThat(response.getStatusCodeValue(), not(401));
        assertThat(response.getStatusCodeValue(), not(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(VALID_CITIZEN_TOKEN)));
    }

    @Test
    public void shouldPassUserAuthorizationWhenValidLetterHolder() {

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL_CITIZEN,
            HttpMethod.GET,
            new HttpEntity<>(validHeaders(VALID_LETTERHOLDER_TOKEN)),
            String.class
        );

        assertThat(response.getStatusCodeValue(), not(401));
        assertThat(response.getStatusCodeValue(), not(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(VALID_LETTERHOLDER_TOKEN)));
    }

    @Test
    public void shouldFailUserAuthorizationWhenInvalidUser() {

        stubFor(get(urlEqualTo("/idam/details"))
                    .withHeader("Authorization", equalTo(INVALID_IDAM_TOKEN))
                    .willReturn(aResponse().withStatus(403)));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", INVALID_IDAM_TOKEN);
        headers.add("ServiceAuthorization", "ServiceToken");
        headers.add("Content-Type", "application/json");

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCodeValue(), is(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(INVALID_IDAM_TOKEN)));
    }

    @Test
    public void shouldFailUserAuthorizationWhenExtractedIdNotMatching() {

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL_WRONG_ID,
            HttpMethod.GET,
            new HttpEntity<>(validHeaders()),
            String.class
        );

        assertThat(response.getStatusCodeValue(), is(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(VALID_IDAM_TOKEN)));
    }

    @Test
    public void shouldFailUserAuthorizationWhenRolesNotMatching() {

        final ResponseEntity<String> response = restTemplate.exchange(
            CASE_URL_WRONG_ROLE,
            HttpMethod.GET,
            new HttpEntity<>(validHeaders()),
            String.class
        );

        assertThat(response.getStatusCodeValue(), is(403));
        verify(getRequestedFor(urlEqualTo("/idam/details"))
                   .withHeader("Authorization", equalTo(VALID_IDAM_TOKEN)));
    }

    private HttpHeaders validHeaders() {
        return validHeaders(VALID_IDAM_TOKEN);
    }

    private HttpHeaders validHeaders(String idamToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", idamToken);
        headers.add("ServiceAuthorization", "ServiceToken");
        headers.add("Content-Type", "application/json");
        return headers;
    }

}
