package uk.gov.hmcts.ccd.config;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("ccd.decentralised")
@Data
public class PersistenceStrategyConfiguration {

    /**
     * A map where the key is the case-type-id and the value is the base URL
     * of the owning service responsible for its persistence.
     * e.g., 'MyCaseType': 'http://my-service-host:port'
     */
    @NotNull
    private Map<String, URI> caseTypeServiceUrls;

}
