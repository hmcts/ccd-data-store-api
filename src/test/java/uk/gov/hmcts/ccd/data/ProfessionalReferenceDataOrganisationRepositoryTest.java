package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalReferenceDataOrganisationRepositoryTest {

    private static final String PRD_API_URL = "http://prd";

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private ProfessionalReferenceDataOrganisationRepository repository;

    @Test
    @DisplayName("should return organisation identifier from PRD response")
    void shouldReturnOrganisationIdentifier() {
        ObjectNode response = new ObjectMapper().createObjectNode()
            .put("organisationIdentifier", "ORG_1");

        when(applicationParams.getPrdApiUrl()).thenReturn(PRD_API_URL);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(eq(PRD_API_URL + "/refdata/external/v1/organisations/users"),
            eq(HttpMethod.GET), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenReturn(ResponseEntity.ok(response));

        Optional<String> organisationIdentifier = repository.getCurrentUserOrganisationIdentifier();

        assertEquals(Optional.of("ORG_1"), organisationIdentifier);
    }

    @Test
    @DisplayName("should return empty when PRD lookup fails")
    void shouldReturnEmptyWhenPrdLookupFails() {
        when(applicationParams.getPrdApiUrl()).thenReturn(PRD_API_URL);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(eq(PRD_API_URL + "/refdata/external/v1/organisations/users"),
            eq(HttpMethod.GET), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        Optional<String> organisationIdentifier = repository.getCurrentUserOrganisationIdentifier();

        assertTrue(organisationIdentifier.isEmpty());
    }

    @Test
    @DisplayName("should return empty when PRD response body is null")
    void shouldReturnEmptyWhenPrdResponseBodyIsNull() {
        when(applicationParams.getPrdApiUrl()).thenReturn(PRD_API_URL);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(eq(PRD_API_URL + "/refdata/external/v1/organisations/users"),
            eq(HttpMethod.GET), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenReturn(ResponseEntity.ok(null));

        Optional<String> organisationIdentifier = repository.getCurrentUserOrganisationIdentifier();

        assertTrue(organisationIdentifier.isEmpty());
    }

    @Test
    @DisplayName("should return empty when organisation identifier is missing")
    void shouldReturnEmptyWhenOrganisationIdentifierMissing() {
        ObjectNode response = new ObjectMapper().createObjectNode()
            .put("users", "ignored");

        when(applicationParams.getPrdApiUrl()).thenReturn(PRD_API_URL);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(eq(PRD_API_URL + "/refdata/external/v1/organisations/users"),
            eq(HttpMethod.GET), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenReturn(ResponseEntity.ok(response));

        Optional<String> organisationIdentifier = repository.getCurrentUserOrganisationIdentifier();

        assertTrue(organisationIdentifier.isEmpty());
    }

    @Test
    @DisplayName("should return empty when organisation identifier is blank")
    void shouldReturnEmptyWhenOrganisationIdentifierBlank() {
        ObjectNode response = new ObjectMapper().createObjectNode()
            .put("organisationIdentifier", "   ");

        when(applicationParams.getPrdApiUrl()).thenReturn(PRD_API_URL);
        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(eq(PRD_API_URL + "/refdata/external/v1/organisations/users"),
            eq(HttpMethod.GET), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenReturn(ResponseEntity.ok(response));

        Optional<String> organisationIdentifier = repository.getCurrentUserOrganisationIdentifier();

        assertTrue(organisationIdentifier.isEmpty());
    }
}
