package uk.gov.hmcts.ccd.data.casedetails;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final String FIND_HASH_FORMAT = "%s%s%s";
    private static final String META_AND_FIELD_DATA_HASH_FORMAT = "%s%s";

    private final CaseDetailsRepository caseDetailsRepository;
    private final Map<Long, Optional<CaseDetails>> idToCaseDetails = newHashMap();
    private final Map<String, Optional<CaseDetails>> referenceToCaseDetails = newHashMap();
    private final Map<String, CaseDetails> findHashToCaseDetails = newHashMap();
    private final Map<String, List<CaseDetails>> metaAndFieldDataHashToCaseDetails = newHashMap();
    private final Map<String, PaginatedSearchMetadata> hashToPaginatedSearchMetadata = newHashMap();

    @Inject
    public CachedCaseDetailsRepository(final @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                               CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        return caseDetailsRepository.set(caseDetails);
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        return idToCaseDetails.computeIfAbsent(id, key -> caseDetailsRepository.findById(jurisdiction, id));
    }

    @Override
    public CaseDetails findById(final Long id) {
        return idToCaseDetails.computeIfAbsent(id, key -> ofNullable(caseDetailsRepository.findById(id))).orElse(null);
    }

    @Override
    public CaseDetails findByReference(final Long caseReference) {
        final Function<String, Optional<CaseDetails>> findFunction = key ->
            ofNullable(caseDetailsRepository.findByReference(caseReference));
        return referenceToCaseDetails.computeIfAbsent(caseReference.toString(), findFunction).orElse(null);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long reference) {
        return findByReference(jurisdiction, reference.toString());
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        return referenceToCaseDetails.computeIfAbsent(reference, key ->
            caseDetailsRepository.findByReference(jurisdiction, reference));
    }

    @Override
    public Optional<CaseDetails> findByReference(String reference) {
        return referenceToCaseDetails.computeIfAbsent(reference, key ->
            caseDetailsRepository.findByReference(reference));
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        return referenceToCaseDetails.computeIfAbsent(reference, key ->
            caseDetailsRepository.findByReferenceWithNoAccessControl(reference));
    }

    @Override
    public CaseDetails findUniqueCase(final String jurisdictionId,
                                      final String caseTypeId,
                                      final String caseReference) {
        return findHashToCaseDetails.computeIfAbsent(
            format(FIND_HASH_FORMAT, jurisdictionId, caseTypeId, caseReference),
            hash -> caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
    }

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return metaAndFieldDataHashToCaseDetails.computeIfAbsent(
            format(META_AND_FIELD_DATA_HASH_FORMAT, metadata.hashCode(), getMapHashCode(dataSearchParams)),
            hash -> caseDetailsRepository.findByMetaDataAndFieldData(metadata, dataSearchParams));
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metadata,
                                                              Map<String, String> dataSearchParams) {
        return hashToPaginatedSearchMetadata.computeIfAbsent(
            format(META_AND_FIELD_DATA_HASH_FORMAT, metadata.hashCode(), getMapHashCode(dataSearchParams)),
            hash -> caseDetailsRepository.getPaginatedSearchMetadata(metadata, dataSearchParams));
    }

    private String getMapHashCode(Map<String, String> dataSearchParams) {
        return dataSearchParams.entrySet()
            .stream()
            .map(entry -> entry.getKey() + entry.getValue())
            .collect(Collectors.joining());
    }
}
