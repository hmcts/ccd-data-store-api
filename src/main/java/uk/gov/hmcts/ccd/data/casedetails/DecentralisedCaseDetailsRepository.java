package uk.gov.hmcts.ccd.data.casedetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@Service
@Slf4j
public class DecentralisedCaseDetailsRepository implements CaseDetailsRepository {

    private final ServicePersistenceClient servicePersistenceAPI;
    private final PersistenceStrategyResolver resolver;

    public DecentralisedCaseDetailsRepository(final ServicePersistenceClient servicePersistenceAPI,
                                             final PersistenceStrategyResolver resolver) {
        this.servicePersistenceAPI = servicePersistenceAPI;
        this.resolver = resolver;
    }

    @Override
    public CaseDetails set(CaseDetails caseDetails) {
        throw new UnsupportedOperationException("Decentralised data is modified through events.");
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CaseDetails findById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> findCaseReferencesByIds(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        return Optional.ofNullable(getCaseDetails(reference));
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        return Optional.ofNullable(getCaseDetails(caseReference.toString()));
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        return Optional.ofNullable(getCaseDetails(reference));
    }

    @Override
    public Optional<CaseDetails> findByReference(String reference) {
        return Optional.ofNullable(getCaseDetails(reference));
    }

    @Override
    public CaseDetails findByReference(Long caseReference) {
        return getCaseDetails(caseReference.toString());
    }

    private CaseDetails getCaseDetails(String reference) {
        var uri = resolver.resolveUriOrThrow(reference);
        CaseDetails caseDetails = servicePersistenceAPI.getCase(uri, reference);
        // TODO: Remove this when legacy RBAC code is removed which relies on details having an id.
        caseDetails.setId(caseDetails.getReference().toString());
        return caseDetails;
    }


    @Override
    public CaseDetails findUniqueCase(String jurisdictionId, String caseTypeId, String caseReference) {
        return getCaseDetails(caseReference);
    }

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(MetaData metadata, Map<String, String> dataSearchParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CaseDetails> findByParamsWithLimit(MigrationParameters migrationParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData, Map<String, String> dataSearchParams) {
        throw new UnsupportedOperationException();
    }
}
