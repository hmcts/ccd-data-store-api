package uk.gov.hmcts.ccd.data.casedetails;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDetailsRepository implements CaseDetailsRepository {

    public static final String QUALIFIER = "cached";

    private final CaseDetailsRepository caseDetailsRepository;

    @Inject
    public CachedCaseDetailsRepository(@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                       final CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        CaseDetails updatedCaseDetails = caseDetailsRepository.set(caseDetails);
        evictSingleCacheValue(updatedCaseDetails.getReferenceAsString());
        return updatedCaseDetails;
    }

    @Override
    @Cacheable(value = "caseDetailsByIDCache", key = "#id")
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        return caseDetailsRepository.findById(jurisdiction, id);
    }

    @Override
    @Cacheable(value = "caseDetailsByIDCache", key = "#id")
    public CaseDetails findById(final Long id) {
        return caseDetailsRepository.findById(id);
    }

    @Override
    public List<Long> findCaseReferencesByIds(final List<Long> ids) {
        return caseDetailsRepository.findCaseReferencesByIds(ids);
    }

    @Override
    @Cacheable(value = "caseDetailsByReferenceCache", key = "#caseReference")
    public CaseDetails findByReference(final Long caseReference) {
        return caseDetailsRepository.findByReference(caseReference);
    }

    @Override
    @Cacheable(value = "caseDetailsByReferenceCache", key = "#reference")
    public Optional<CaseDetails> findByReference(String jurisdiction, Long reference) {
        return this.findByReference(jurisdiction, reference.toString());
    }

    @Override
    @Cacheable(value = "caseDetailsByReferenceCache", key = "#reference")
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        return caseDetailsRepository.findByReference(jurisdiction, reference);
    }

    @Override
    @Cacheable(value = "caseDetailsByReferenceCache", key = "#reference")
    public Optional<CaseDetails> findByReference(String reference) {
        return caseDetailsRepository.findByReference(reference);
    }

    @Override
    @Cacheable(value = "caseDetailsByReferenceCache", key = "#reference")
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        return caseDetailsRepository.findByReferenceWithNoAccessControl(reference);
    }

    @Override
    @Cacheable("uniqueCaseDetailsCache")
    public CaseDetails findUniqueCase(final String jurisdictionId,
                                      final String caseTypeId,
                                      final String caseReference) {
        return caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference);
    }

    @Override
    @Cacheable(value = "caseDetailsByMetaDataAndFieldDataCache", keyGenerator = "caseDetailsKeyGenerator")
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return caseDetailsRepository.findByMetaDataAndFieldData(metadata, dataSearchParams);
    }

    @Override
    @Cacheable(value = "paginatedSearchMetadataCache", keyGenerator = "caseDetailsKeyGenerator")
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metadata,
                                                              Map<String, String> dataSearchParams) {
        return caseDetailsRepository.getPaginatedSearchMetadata(metadata, dataSearchParams);
    }

    @CacheEvict(value = {"caseDetailsByReferenceCache"}, key = "#reference", allEntries = true)
    public void evictSingleCacheValue(String reference) {
    }

}
