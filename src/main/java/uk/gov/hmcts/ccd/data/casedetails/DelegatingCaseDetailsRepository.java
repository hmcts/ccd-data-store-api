package uk.gov.hmcts.ccd.data.casedetails;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RequiredArgsConstructor
@Qualifier(DelegatingCaseDetailsRepository.QUALIFIER)
@Service
public class DelegatingCaseDetailsRepository implements CaseDetailsRepository {

    private final PersistenceStrategyResolver resolver;
    private final DecentralisedCaseDetailsRepository decentralisedCaseDetailsRepository;
    private final DefaultCaseDetailsRepository defaultRepository;
    public static final String QUALIFIER = "delegating";

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        return defaultRepository.set(caseDetails);
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        if (resolver.isDecentralised(id)) {
            return decentralisedCaseDetailsRepository.findById(jurisdiction, id);
        } else {
            return defaultRepository.findById(jurisdiction, id);
        }
    }

    @Override
    @Deprecated
    public CaseDetails findById(final Long id) {
        if (resolver.isDecentralised(id)) {
            return decentralisedCaseDetailsRepository.findById(id);
        } else {
            return defaultRepository.findById(id);
        }
    }

    @Override
    public List<Long> findCaseReferencesByIds(final List<Long> ids) {
        // Case references and ids are stored locally.
        return defaultRepository.findCaseReferencesByIds(ids);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(jurisdiction, caseReference);
        } else {
            return defaultRepository.findByReference(jurisdiction, caseReference);
        }
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findByReference(jurisdiction, reference);
        } else {
            return defaultRepository.findByReference(jurisdiction, reference);
        }
    }

    @Override
    public Optional<CaseDetails> findByReference(String caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(caseReference);
        } else {
            return defaultRepository.findByReference(caseReference);
        }
    }

    /**
     * @param caseReference Public case reference
     * @return Case details if found; null otherwise.
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findByReference(final Long caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(caseReference);
        } else {
            return defaultRepository.findByReference(caseReference);
        }
    }

    /**
     * @param jurisdiction Jurisdiction's ID
     * @param caseTypeId   Case's type ID
     * @param reference    Case unique 16-digit reference
     * @return Case details if found; null otherwise
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, String)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findUniqueCase(final String jurisdiction,
                                      final String caseTypeId,
                                      final String reference) {
       if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findUniqueCase(jurisdiction, caseTypeId, reference);
        } else {
            return defaultRepository.findUniqueCase(jurisdiction, caseTypeId, reference);
        }
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findByReferenceWithNoAccessControl(reference);
        } else {
            return defaultRepository.findByReferenceWithNoAccessControl(reference);
        }
    }

    /**
     * Legacy postgres based search method; Decentralised case types cannot use it.
     */
    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return defaultRepository.findByMetaDataAndFieldData(metadata, dataSearchParams);
    }

    /**
     * Legacy postgres based search method; Decentralised case types cannot use it.
     */
    @Override
    public List<CaseDetails> findByParamsWithLimit(final MigrationParameters migrationParameters) {
        return defaultRepository.findByParamsWithLimit(migrationParameters);
    }

    /**
     * Legacy postgres based search method; Decentralised case types cannot use it.
     */
    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData,
                                                              Map<String, String> dataSearchParams) {
        return defaultRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);
    }

}
