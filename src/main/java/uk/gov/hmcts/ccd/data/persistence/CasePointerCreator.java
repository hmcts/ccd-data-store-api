package uk.gov.hmcts.ccd.data.persistence;


import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;

/**
 * Creates 'case pointers' in the case_data table for decentralised case types.
 * Case pointers contain no case data and allow CCD to route requests to the appropriate decentralised service.
 */
@RequiredArgsConstructor
@Service
public class CasePointerCreator {

    private final DefaultCaseDetailsRepository caseDetailsRepository;
    private final CaseService caseService;

    /**
     * Persists the immutable case pointer in a new, separate transaction.
     * This transaction will commit immediately upon successful completion of this method.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCasePointer(CaseDetails caseDetails) {
        CaseDetails pointer = caseService.clone(caseDetails);
        pointer.setData(Map.of());
        pointer.setSecurityClassification(SecurityClassification.RESTRICTED);
        pointer.setLastModified(null);
        pointer.setVersion(null);
        pointer.setDataClassification(null);
        pointer.setState("");
        var persisted = caseDetailsRepository.set(pointer);
        // We need the ID that the database has allocated us.
        caseDetails.setId(persisted.getId());
    }
}
