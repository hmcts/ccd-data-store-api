package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.Optional;

@Repository
public class ProfessionalReferenceDataOrganisationRepository {

    static final String ORGANISATIONS_USERS_PATH = "/refdata/external/v1/organisations/users";
    private static final String ORGANISATION_IDENTIFIER_FIELD = "organisationIdentifier";

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;

    public ProfessionalReferenceDataOrganisationRepository(SecurityUtils securityUtils,
                                                           @Qualifier("restTemplate") RestTemplate restTemplate,
                                                           ApplicationParams applicationParams) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
    }

    public Optional<String> getCurrentUserOrganisationIdentifier() {
        try {
            JsonNode response = restTemplate.exchange(
                applicationParams.getPrdApiUrl() + ORGANISATIONS_USERS_PATH,
                HttpMethod.GET,
                new HttpEntity<>(securityUtils.authorizationHeaders()),
                JsonNode.class
            ).getBody();

            if (response == null || response.get(ORGANISATION_IDENTIFIER_FIELD) == null) {
                return Optional.empty();
            }

            String organisationIdentifier = response.get(ORGANISATION_IDENTIFIER_FIELD).asText();
            return Optional.ofNullable(organisationIdentifier).filter(value -> !value.isBlank());
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }
}
