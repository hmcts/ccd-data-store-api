package uk.gov.hmcts.ccd.domain.service.common;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

/**
 * Resolves the persistence strategy for a given case.
 *
 * This service determines whether a case's mutable data should be handled by the
 * internal CCD database or delegated to an external, case-type-specific service.
 */
@Service
@Slf4j
@ConfigurationProperties("ccd.decentralised")
public class PersistenceStrategyResolver {

    private final DefaultCaseDetailsRepository caseDetailsRepository;
    private final Cache<Long, String> caseTypeCache;

    /**
     * A map where the key is lowercase(case-type-id) and the value is the base URL
     * of the owning service responsible for its persistence.
     * e.g., 'MyCaseType': 'http://my-service-host:port'
     *
     * This can be set via application properties and environment variables.
     */
    @NotNull
    private Map<String, URI> caseTypeServiceUrls;


    @Autowired
    public PersistenceStrategyResolver(DefaultCaseDetailsRepository caseDetailsRepository ) {
        this.caseDetailsRepository = caseDetailsRepository;
        // Least Recently Used cache for lookup of case type by reference.
        // At around 100 bytes per entry this cache will use up to 10MB of memory.
        // https://github.com/ben-manes/caffeine/wiki/Memory-overhead
        this.caseTypeCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build();
    }

    /**
     * Sets the case type service URLs when the application starts.
     * The keys in the map are converted to lowercase to ensure case-insensitivity.
     */
    public void setCaseTypeServiceUrls(Map<String, URI> caseTypeServiceUrls) {
        this.caseTypeServiceUrls = new HashMap<>();
        caseTypeServiceUrls.forEach((key, value) ->
            this.caseTypeServiceUrls.put(key.toLowerCase(), value)
        );
    }

    public boolean isDecentralised(CaseDetails details) {
        return getCaseTypeServiceUrl(details.getCaseTypeId()).isPresent();
    }

    public boolean isDecentralised(Long reference) {
        var caseType = getCaseTypeByReference(reference);
        return getCaseTypeServiceUrl(caseType).isPresent();
    }

    public URI resolveUriOrThrow(Long caseReference) {
        var caseType = getCaseTypeByReference(caseReference);

        return getCaseTypeServiceUrl(caseType).orElseThrow();
    }


    public URI resolveUriOrThrow(CaseDetails caseDetails) {
        Optional<URI> url = getCaseTypeServiceUrl(caseDetails.getCaseTypeId());
        if (url.isPresent()) {
            return url.get();
        }
        String message = String.format(
            "Operation failed: No decentralised persistence route configured for case type %s",
            caseDetails.getCaseTypeId()
        );
        throw new UnsupportedOperationException(message);
    }


    private Optional<URI> getCaseTypeServiceUrl(String caseTypeId) {
        return Optional.ofNullable(caseTypeServiceUrls.get(caseTypeId.toLowerCase()));
    }

    private String getCaseTypeByReference(Long reference) {
        return caseTypeCache.get(reference, this.caseDetailsRepository::findCaseTypeByReference);
    }
}
