package uk.gov.hmcts.ccd.data.persistence;


import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

/**
 * Creates 'case pointers' in the case_data table for decentralised case types.
 * Case pointers contain no case data and allow CCD to route requests to the appropriate decentralised service.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CasePointerRepository {

    private final DefaultCaseDetailsRepository caseDetailsRepository;
    private final CaseService caseService;
    private final EntityManager em;

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

    public String findCaseTypeByReference(Long caseReference) {
        try {
            return em.createQuery(
                    "SELECT cd.caseType FROM CaseDetailsEntity cd WHERE cd.reference = :reference",
                    String.class
                ).setParameter("reference", caseReference)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("No case found for reference: " + caseReference, e);
        }
    }

    public void updateResolvedTtl(Long caseReference, LocalDate resolvedTtl) {
        log.info("Updating resolved TTL for caseReference {}: {}", caseReference, resolvedTtl);
        em.createQuery("UPDATE CaseDetailsEntity SET resolvedTTL = :ttl WHERE reference = :caseReference")
            .setParameter("ttl", resolvedTtl)
            .setParameter("caseReference", caseReference)
            .executeUpdate();
    }
}
