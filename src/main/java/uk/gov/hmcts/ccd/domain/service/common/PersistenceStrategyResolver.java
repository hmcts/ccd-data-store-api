package uk.gov.hmcts.ccd.domain.service.common;

import java.net.URI;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.PersistenceStrategyConfiguration;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
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
public class PersistenceStrategyResolver {

    private final PersistenceStrategyConfiguration config;
    private final CaseDetailsRepository caseDetailsRepository;

    @Autowired
    public PersistenceStrategyResolver(
        PersistenceStrategyConfiguration decentralisedConfiguration,
        @Qualifier(DefaultCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository
    ) {
        this.config = decentralisedConfiguration;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    public boolean isDecentralised(CaseDetails details) {
        return config.getCaseTypeServiceUrls().containsKey(details.getCaseTypeId());
    }

    public boolean isDecentralised(long reference) {
        return isDecentralised(String.valueOf(reference));
    }

    public boolean isDecentralised(String reference) {
        var details = caseDetailsRepository.findByReferenceWithNoAccessControl(reference).get();
        return config.getCaseTypeServiceUrls().containsKey(details.getCaseTypeId());
    }



    /**
     * Resolves the persistence strategy for a given case reference.
     * <p>
     * It first retrieves the case details to determine the case type.
     * The `CachedCaseDetailsRepository` is used to ensure this lookup is efficient.
     * It then checks the configuration to see if the case type has a decentralised
     * persistence URL associated with it.
     *
     * @param caseReference The unique 16-digit reference of the case.
     * @return An {@code Optional<String>} containing the base URL of the owning
     *         service if the case type is decentralised. Otherwise, returns
     *         an empty Optional to indicate that the case should be handled
     *         by the internal CCD database.
     */
    public Optional<URI> resolveUrl(String caseReference) {

        // 1. Get CaseDetails using the repository. The injected 'cached' version handles caching.
        Optional<CaseDetails> caseDetailsOptional = caseDetailsRepository.findByReference(caseReference);

        if (caseDetailsOptional.isEmpty()) {
            log.warn("Could not find case details for reference: {}. Assuming centralised persistence.", caseReference);
            return Optional.empty();
        }

        Optional<URI> url = Optional.ofNullable(config.getCaseTypeServiceUrls().get(caseDetailsOptional.get().getCaseTypeId()));
        return url;
    }

    public URI resolveUriOrThrow(String caseReference) {

        // 1. Get CaseDetails using the repository. The injected 'cached' version handles caching.
        Optional<CaseDetails> caseDetailsOptional = caseDetailsRepository.findByReference(caseReference);

        if (caseDetailsOptional.isEmpty()) {
            // TODO
            log.warn("Could not find case details for reference: {}. Assuming centralised persistence.", caseReference);
            throw new UnsupportedOperationException();
        }

        return config.getCaseTypeServiceUrls().get(caseDetailsOptional.get().getCaseTypeId());
    }


    public URI resolveUriOrThrow(CaseDetails caseDetails) {
        Optional<URI> url = Optional.ofNullable(config.getCaseTypeServiceUrls().get(caseDetails.getCaseTypeId()));
        if (url.isPresent()) {
            return url.get();
        }
        String message = String.format(
            "Operation failed: No decentralised persistence route configured for case type %s",
            caseDetails.getCaseTypeId()
        );
        throw new UnsupportedOperationException(message);
    }
}
