package uk.gov.hmcts.ccd.integrations;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class IdamIT extends IntegrationTest {

    private static final String CASE_URL = "/caseworkers/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_CITIZEN = "/citizens/123/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_WRONG_ID = "/caseworkers/456/jurisdictions/TEST/case-types/TestAddressBook/cases/1234123412341238";
    private static final String CASE_URL_WRONG_ROLE = "/caseworkers/123/jurisdictions/PROBATE/case-types/TestAddressBook/cases/1234123412341238";

    private static final String VALID_IDAM_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiRm8rQXAybThDT3ROb290ZjF4TWg0bGc3MFlBPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjY2RpbXBvcnRkb21haW5AZ21haWwuY29tIiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiODdkNmFjNTItZDNiNi00ZmRhLTg3NmEtNzljNjc5MTc1MGZhIiwiaXNzIjoiaHR0cHM6Ly9mb3JnZXJvY2stYW0uc2VydmljZS5jb3JlLWNvbXB1dGUtaWRhbS1hYXQuaW50ZXJuYWw6ODQ0My9vcGVuYW0vb2F1dGgyL2htY3RzIiwidG9rZW5OYW1lIjoiYWNjZXNzX3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFudElkIjoiNDIxMDE3OGQtYWNkZi00NmJlLWI5Y2UtMzEyMTUzYTU1MTBhIiwiYXVkIjoiY2NkX2FkbWluIiwibmJmIjoxNTgyNTg1OTMyLCJncmFudF90eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSIsInJvbGVzIl0sImF1dGhfdGltZSI6MTU4MjU4NTkzMjAwMCwicmVhbG0iOiIvaG1jdHMiLCJleHAiOjE1ODI2MTQ3MzIsImlhdCI6MTU4MjU4NTkzMiwiZXhwaXJlc19pbiI6Mjg4MDAsImp0aSI6IjI4YjQ4NjM3LWQ4OTQtNGZmYi1iNTRhLTg3ZGJhY2M5ZTcyNSJ9.mloB_wJy1rwFxhRa5bFoCzBi0_x79EqZb6Z4J057Eya9MAsvmQoaYoQYVvUqxY3-od4hXTmA5XfaCcorHEmFiuNsfT0mm0bKWbdBDG6DIU2Ef1ekSf60p9i35-j7OEfR3ps8KRcEQtIYGjBj7io9KWjWyTYsE9JUvgz4kBdwA8w5T2jBJZCOOZANfOIrhXHHxpTvtNmnPq_nCHsxs_ymNULpODOC7_UzKVCLmYp-ClioXesst-swVUHRBDF2r-wEIS2iR0r3qBFkZ8wBEOHRugrZPFAxRv1yWwRzUwTijayMaYT24iqVqECNFe0BtPigrT8SxpNaZK26ZpUA8B5aOQ";
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
