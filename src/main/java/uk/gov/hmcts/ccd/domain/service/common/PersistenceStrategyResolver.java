package uk.gov.hmcts.ccd.domain.service.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.CasePointerRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Determines whether a case's mutable data is handled by the
 * internal CCD database or delegated to an external, service implemented persistence handler.
 * This implementation uses prefix-based matching for case types to support the preview environment.
 */
@Service
@Slf4j
@ConfigurationProperties("ccd.decentralised")
public class PersistenceStrategyResolver {

    private final CasePointerRepository pointerRepository;
    private final Cache<Long, String> caseTypeCache;

    /**
     * A map where the key is a lowercase(case-type-id-prefix) and the value is the base URL
     * of the owning service responsible for its persistence.
     * e.g., 'mycasetype': 'http://my-service-host:port' would match 'MyCaseType-1234'.
     *
     * <p>This can be set via application properties and environment variables.
     */
    @NotNull
    private Map<String, URI> caseTypeServiceUrls = Map.of();


    @Autowired
    public PersistenceStrategyResolver(CasePointerRepository pointerRepository) {
        this.pointerRepository = pointerRepository;
        // Least Recently Used cache for lookup of case type by reference.
        // At around 100 bytes per entry this cache will use up to 10MB of memory
        // while comfortably accommodating the current working set of cases.
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
        if (this.caseTypeServiceUrls.isEmpty()) {
            log.info("No decentralised persistence URLs configured.");
        } else {
            log.info("Loaded {} decentralised case type route(s) for prefixes: {}",
                this.caseTypeServiceUrls.size(),
                this.caseTypeServiceUrls.keySet());
        }
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

        return getCaseTypeServiceUrl(caseType).orElseThrow(() -> {
            String message = String.format(
                "No decentralised persistence route configured for case type %s (from case reference %d)",
                caseType,
                caseReference
            );
            return new UnsupportedOperationException(message);
        });
    }


    public URI resolveUriOrThrow(CaseDetails caseDetails) {
        return getCaseTypeServiceUrl(caseDetails.getCaseTypeId()).orElseThrow(() -> {
            String message = String.format(
                "Operation failed: No decentralised persistence route configured for case type %s",
                caseDetails.getCaseTypeId()
            );
            return new UnsupportedOperationException(message);
        });
    }

    /**
     * Finds the persistence service URL for a given case type ID using prefix matching.
     *
     * @param caseTypeId The full case type ID (e.g., "NFD").
     * @return An {@link Optional} containing the {@link URI} if a single, unambiguous prefix match is found.
     * @throws IllegalStateException if more than one configured case type matches the given caseTypeId.
     */
    private Optional<URI> getCaseTypeServiceUrl(String caseTypeId) {
        if (caseTypeId == null || caseTypeId.isBlank()) {
            log.debug("Cannot resolve persistence strategy for null or blank case type ID.");
            return Optional.empty();
        }

        final String lowerCaseTypeId = caseTypeId.toLowerCase();

        List<String> matchingPrefixes = caseTypeServiceUrls.keySet().stream()
            .filter(lowerCaseTypeId::startsWith)
            .collect(Collectors.toList());

        if (matchingPrefixes.size() > 1) {
            String conflictingPrefixes = String.join(", ", matchingPrefixes);
            String message = String.format(
                "Ambiguous configuration for case type '%s'. Multiple prefix matches found: [%s]",
                caseTypeId,
                conflictingPrefixes
            );
            log.error(message);
            throw new IllegalStateException(message);
        }

        Optional<String> matchingPrefixOptional = matchingPrefixes.stream().findFirst();

        if (matchingPrefixOptional.isPresent()) {
            String prefix = matchingPrefixOptional.get();
            URI url = caseTypeServiceUrls.get(prefix);
            log.debug("Case type '{}' matches decentralised persistence rule with prefix '{}'. Routing to: {}",
                caseTypeId,
                prefix,
                url);
            return Optional.of(url);
        } else {
            log.debug("Case type '{}' is not configured for decentralised persistence. Using default.", caseTypeId);
            return Optional.empty();
        }
    }

    private String getCaseTypeByReference(Long reference) {
        return caseTypeCache.get(reference, ref -> {
            log.debug("Cache miss for case reference: {}. Looking up case type from repository.", ref);
            return this.pointerRepository.findCaseTypeByReference(ref);
        });
    }
}
