package uk.gov.hmcts.ccd.data.casedetails;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

@Service
@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDetailsRepository implements CaseDetailsRepository {

    public static final String QUALIFIER = "cached";
    private static final String FIND_HASH_FORMAT = "%s%s%s";
    private static final String META_AND_FIELD_DATA_HASH_FORMAT = "%s%s";

    private final CaseDetailsRepository caseDetailsRepository;
    private final Map<Long, CaseDetails> idToCaseDetails = newHashMap();
    private final Map<Long, CaseDetails> referenceToCaseDetails = newHashMap();
    private final Map<String, CaseDetails> findHashToCaseDetails = newHashMap();
    private final Map<String, List<CaseDetails>> metaAndFieldDataHashToCaseDetails = newHashMap();
    private final Map<String, PaginatedSearchMetadata> hashToPaginatedSearchMetadata = newHashMap();

    @Inject
    public CachedCaseDetailsRepository(@Qualifier(DefaultCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        return caseDetailsRepository.set(caseDetails);
    }

    @Override
    public CaseDetails findById(final Long id) {
        return idToCaseDetails.computeIfAbsent(id, caseDetailsRepository::findById);
    }

    @Override
    public CaseDetails findByReference(final Long caseReference) {
        return referenceToCaseDetails.computeIfAbsent(caseReference, caseDetailsRepository::findByReference);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdictionId, Long caseReference) {
        if (referenceToCaseDetails.containsKey(caseReference)) {
            return Optional.ofNullable(referenceToCaseDetails.get(caseReference))
                           .filter(caseDetails -> jurisdictionId.equals(caseDetails.getJurisdiction()));
        } else {
            final Optional<CaseDetails> optionalCaseDetails = caseDetailsRepository.findByReference(jurisdictionId,
                                                                                                    caseReference);
            referenceToCaseDetails.put(caseReference, optionalCaseDetails.orElse(null));
            return optionalCaseDetails;
        }
    }

    @Override
    public CaseDetails lockCase(final Long caseReference) {
        return caseDetailsRepository.lockCase(caseReference);
    }

    @Override
    public CaseDetails findUniqueCase(final String jurisdictionId,
                                      final String caseTypeId,
                                      final String caseReference) {
        return findHashToCaseDetails.computeIfAbsent(format(FIND_HASH_FORMAT, jurisdictionId, caseTypeId, caseReference),
                                                            hash -> caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
    }

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata, final Map<String, String> dataSearchParams) {
        return metaAndFieldDataHashToCaseDetails.computeIfAbsent(format(META_AND_FIELD_DATA_HASH_FORMAT,
                                                                        metadata.hashCode(),
                                                                        getMapHashCode(dataSearchParams)),
                                                                        hash -> caseDetailsRepository.findByMetaDataAndFieldData(
                                                                            metadata,
                                                                            dataSearchParams));
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metadata, Map<String, String> dataSearchParams) {
        return hashToPaginatedSearchMetadata.computeIfAbsent(format(META_AND_FIELD_DATA_HASH_FORMAT,
                                                                    metadata.hashCode(),
                                                                    getMapHashCode(dataSearchParams)),
                                                             hash -> caseDetailsRepository.getPaginatedSearchMetadata(
                                                                 metadata,
                                                                 dataSearchParams
                                                             ));
    }

    private String getMapHashCode(Map<String, String> dataSearchParams) {
        return dataSearchParams.entrySet()
            .stream()
            .map(entry -> entry.getKey() + entry.getValue())
            .collect(Collectors.joining());
    }
}
