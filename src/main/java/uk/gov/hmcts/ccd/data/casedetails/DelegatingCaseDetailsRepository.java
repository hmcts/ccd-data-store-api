package uk.gov.hmcts.ccd.data.casedetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A delegating repository that acts as a router between the local (Postgres)
 * and decentralised (remote service) case data stores.
 *
 * The routing strategy is 'local first' delegating first to the local repository
 * and only if the resulting case type is decentralised delegating to the relevant remote service.
 *
 * This allows us to reuse the existing local repository access control and private case ID lookups.
 *
 * Where cases are centralised this avoids additional database round trips.
 * Where cases are decentralised the local case details will be a lightweight metadata 'shell'.
 */
@Slf4j
@Service
@Qualifier(DelegatingCaseDetailsRepository.QUALIFIER)
@RequiredArgsConstructor
public class DelegatingCaseDetailsRepository implements CaseDetailsRepository {

    public static final String QUALIFIER = "delegating";

    private final PersistenceStrategyResolver resolver;
    private final ServicePersistenceClient decentralisedClient;
    private final DefaultCaseDetailsRepository localRepository;

    @Override
    public CaseDetails set(CaseDetails caseDetails) {
        if (resolver.isDecentralised(caseDetails)) {
            throw new UnsupportedOperationException(
                String.format("""
                        Case type '%s' is decentralised and managed by the owning service.
                        CCD's Decentralised CaseDetails is an immutable pointer.""",
                    caseDetails.getCaseTypeId())
            );
        }
        return localRepository.set(caseDetails);
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        return findAndDelegate(
            () -> localRepository.findById(jurisdiction, id),
            decentralisedClient::getCase
        );
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        return findAndDelegate(
            () -> localRepository.findByReference(jurisdiction, caseReference),
            decentralisedClient::getCase
        );
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        return findAndDelegate(
            () -> localRepository.findByReferenceWithNoAccessControl(reference),
            decentralisedClient::getCase
        );
    }

    /**
     * Centralised delegation logic; route to the local repository first,
     * then delegate to the decentralised service if the case type is decentralised.
     */
    private Optional<CaseDetails> findAndDelegate(Supplier<Optional<CaseDetails>> localCaseFinder,
                                                  Function<CaseDetails, CaseDetails> decentralisedFinder) {
        return localCaseFinder.get().map(shellCase -> {
            if (resolver.isDecentralised(shellCase)) {
                log.debug("Case reference '{}' is decentralised. Delegating to remote service.", shellCase.getReference());
                return decentralisedFinder.apply(shellCase);
            }
            return shellCase;
        });
    }

    // Overloaded and Deprecated methods
    // These delegate to the primary methods above.

    @Override
    @Deprecated
    public CaseDetails findById(Long id) {
        return findById(null, id).orElse(null);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        return findByReference(jurisdiction, Long.parseLong(reference));
    }

    @Override
    public Optional<CaseDetails> findByReference(String caseReference) {
        return findByReference(null, Long.parseLong(caseReference));
    }

    @Override
    @Deprecated
    public CaseDetails findByReference(Long caseReference) {
        return findByReference(null, caseReference).orElseThrow(() -> new ResourceNotFoundException("No case found"));
    }

    @Override
    @Deprecated
    public CaseDetails findUniqueCase(String jurisdiction, String caseTypeId, String reference) {
        return findByReference(jurisdiction, Long.parseLong(reference)).orElse(null);
    }

    @Override
    public List<Long> findCaseReferencesByIds(List<Long> ids) {
        // Case references and IDs are always stored locally.
        return localRepository.findCaseReferencesByIds(ids);
    }

    /**
     * Legacy postgres-based search method. Decentralised case types do not support this.
     */
    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(MetaData metadata,
                                                        Map<String, String> dataSearchParams) {
        return localRepository.findByMetaDataAndFieldData(metadata, dataSearchParams);
    }

    /**
     * Legacy postgres-based search method for migration. Decentralised case types do not support this.
     */
    @Override
    public List<CaseDetails> findByParamsWithLimit(MigrationParameters migrationParameters) {
        return localRepository.findByParamsWithLimit(migrationParameters);
    }

    /**
     * Legacy postgres-based search method. Decentralised case types do not support this.
     */
    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData,
                                                              Map<String, String> dataSearchParams) {
        return localRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);
    }
}
