package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CaseDetailsRepository {
    CaseDetails set(CaseDetails caseDetails);

    /**
     *
     * @param id Internal case ID
     * @return Case details
     * @deprecated Case retrieval should be done by reference. Use {@link CaseDetailsRepository#findByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails findById(Long id);

    /**
     *
     * @param caseReference Public case reference
     * @return Case details
     * @deprecated Case retrieval should be done by reference. Use {@link CaseDetailsRepository#findByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails findByReference(Long caseReference);

    Optional<CaseDetails> findByReference(String jurisdictionId, Long caseReference);

    CaseDetails lockCase(Long caseReference);

    /**
     *
     * @param jurisdictionId Case's jurisdiction ID
     * @param caseTypeId Case's type ID
     * @param caseReference Public case reference
     * @return Case details
     * @deprecated Case retrieval should be done by reference. Use {@link CaseDetailsRepository#findByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails findUniqueCase(String jurisdictionId,
                               String caseTypeId,
                               String caseReference);

    List<CaseDetails> findByMetaDataAndFieldData(MetaData metadata, Map<String, String> dataSearchParams);

    PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData, Map<String, String> dataSearchParams);
}
