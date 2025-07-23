package uk.gov.hmcts.ccd.data.casedetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Qualifier(DelegatingCaseDetailsRepository.QUALIFIER)
@RequiredArgsConstructor
public class DelegatingCaseDetailsRepository implements CaseDetailsRepository {

    public static final String QUALIFIER = "delegating";
    private final PersistenceStrategyResolver resolver;
    private final DecentralisedCaseDetailsRepository decentralisedRepository;
    private final DefaultCaseDetailsRepository localRepository;

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
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
        Optional<CaseDetails> localResult = localRepository.findById(jurisdiction, id);

        return localResult.flatMap(caseDetails -> {
            if (resolver.isDecentralised(caseDetails)) {
                log.debug("Case ID '{}' is decentralised. Delegating to remote service.", id);
                // Use the reference from the shell case to fetch from the decentralised service.
                return decentralisedRepository.findByReference(caseDetails.getJurisdiction(), caseDetails.getReference());
            } else {
                // It's a local case, the result we already have is the complete one.
                return Optional.of(caseDetails);
            }
        });
    }

    @Override
    @Deprecated
    public CaseDetails findById(final Long id) {
        return findById(null, id).orElse(null);
    }

    @Override
    public List<Long> findCaseReferencesByIds(final List<Long> ids) {
        // Case references and IDs are always stored locally.
        return localRepository.findCaseReferencesByIds(ids);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        Optional<CaseDetails> localResult = localRepository.findByReference(jurisdiction, caseReference);

        return localResult.flatMap(caseDetails -> {
            if (resolver.isDecentralised(caseDetails)) {
                log.debug("Case reference '{}' is decentralised. Delegating to remote service.", caseReference);
                return decentralisedRepository.findByReference(jurisdiction, caseReference);
            } else {
                return Optional.of(caseDetails);
            }
        });
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
    public CaseDetails findByReference(final Long caseReference) {
        return findByReference(null, caseReference).orElse(null);
    }

    @Override
    @Deprecated
    public CaseDetails findUniqueCase(final String jurisdiction, final String caseTypeId, final String reference) {
        return findByReference(jurisdiction, Long.parseLong(reference)).orElse(null);
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        Optional<CaseDetails> localResult = localRepository.findByReferenceWithNoAccessControl(reference);

        return localResult.flatMap(caseDetails -> {
            if (resolver.isDecentralised(caseDetails)) {
                log.debug("Found shell for reference {} with no access control, delegating to remote service.", reference);
                return decentralisedRepository.findByReferenceWithNoAccessControl(reference);
            } else {
                return Optional.of(caseDetails);
            }
        });
    }

    /**
     * Legacy postgres-based search method; Decentralised case types do not support this.
     */
    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return localRepository.findByMetaDataAndFieldData(metadata, dataSearchParams);
    }

    /**
     * Legacy postgres-based search method; Decentralised case types do not support this.
     */
    @Override
    public List<CaseDetails> findByParamsWithLimit(final MigrationParameters migrationParameters) {
        return localRepository.findByParamsWithLimit(migrationParameters);
    }

    /**
     * Legacy postgres-based search method; Decentralised case types do not support this.
     */
    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData,
                                                              Map<String, String> dataSearchParams) {
        return localRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);
    }
}
