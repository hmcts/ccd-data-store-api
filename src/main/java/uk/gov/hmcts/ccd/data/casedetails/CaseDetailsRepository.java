package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CaseDetailsRepository {
    CaseDetails set(CaseDetails caseDetails);

    Optional<CaseDetails> findById(String jurisdiction, Long id);

    Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference);

    Optional<CaseDetails> findByReference(String jurisdiction, String reference);

    Optional<CaseDetails> lockByReference(String jurisdiction, Long reference);

    Optional<CaseDetails> lockByReference(String jurisdiction, String reference);

    /**
     *
     * @param id Internal case ID
     * @return Case details
     * @deprecated Use {@link CaseDetailsRepository#findByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails findById(Long id);

    /**
     *
     * @param caseReference Public case reference
     * @return Case details
     * @deprecated Use {@link CaseDetailsRepository#findByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails findByReference(Long caseReference);

    /**
     *
     * @param caseReference Public case reference
     * @return Case details
     * @deprecated Use {@link CaseDetailsRepository#lockByReference(String, Long)} instead.
     */
    @Deprecated
    CaseDetails lockCase(Long caseReference);

    /**
     *
     * @param jurisdictionId Case's jurisdiction ID
     * @param caseTypeId Case's type ID
     * @param caseReference Public case reference
     * @return Case details
     * @deprecated Use {@link CaseDetailsRepository#findByReference(String, String)} instead.
     */
    @Deprecated
    CaseDetails findUniqueCase(String jurisdictionId,
                               String caseTypeId,
                               String caseReference);

    List<CaseDetails> findByMetaDataAndFieldData(MetaData metadata, Map<String, String> dataSearchParams);

    PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData, Map<String, String> dataSearchParams);
}
