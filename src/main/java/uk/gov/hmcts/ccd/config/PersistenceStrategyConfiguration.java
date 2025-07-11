package uk.gov.hmcts.ccd.config;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("ccd.decentralised")
public class PersistenceStrategyConfiguration {

    /**
     * A map where the key is lowercase(case-type-id) and the value is the base URL
     * of the owning service responsible for its persistence.
     * e.g., 'MyCaseType': 'http://my-service-host:port'
     */
    @NotNull
    private Map<String, URI> caseTypeServiceUrls;

    public Optional<URI> getCaseTypeServiceUrl(String caseTypeId) {
        return Optional.ofNullable(caseTypeServiceUrls.get(caseTypeId.toLowerCase()));
    }

    public void setCaseTypeServiceUrls(Map<String, URI> caseTypeServiceUrls) {
        this.caseTypeServiceUrls = new HashMap<>();
        caseTypeServiceUrls.forEach((key, value) ->
            this.caseTypeServiceUrls.put(key.toLowerCase(), value)
        );
    }
}
