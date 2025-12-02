package uk.gov.hmcts.ccd.data.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
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

    private static final long DANGLING_POINTER_EXPIRY_TIMEOUT_YEARS = 1L;

    private final DefaultCaseDetailsRepository caseDetailsRepository;
    private final CaseService caseService;
    private final EntityManager em;

    /**
     * Persists the immutable case pointer in a new, separate transaction.
     * This transaction will commit immediately upon successful completion of this method.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCasePointerAndInitId(CaseDetails caseDetails) {
        CaseDetails pointer = caseService.clone(caseDetails);
        pointer.setData(Map.of());
        pointer.setDataClassification(Map.of());
        pointer.setSecurityClassification(SecurityClassification.RESTRICTED);
        pointer.setLastModified(null);
        pointer.setLastStateModifiedDate(null);
        pointer.setVersion(null);
        pointer.setState("");
        if (pointer.getResolvedTTL() == null) {
            // Default a case pointer expiry so dangling pointers are always eventually cleaned up
            pointer.setResolvedTTL(LocalDate.now().plusYears(DANGLING_POINTER_EXPIRY_TIMEOUT_YEARS));
        }
        var result = caseDetailsRepository.set(pointer);
        caseDetails.setId(result.getId());
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

    /**
     * Ensure a case pointer is deleted regardless of the state of any outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCasePointer(Long caseReference) {
        log.info("Deleting case pointer for caseReference: {}", caseReference);
        // Case pointers are stored with empty data, as an additional precaution.
        int deletedCount = em.createNativeQuery("""
                delete from case_data where reference = :caseReference and data = cast('{}' as jsonb)
                """)
            .setParameter("caseReference", caseReference)
            .executeUpdate();

        if (deletedCount > 0) {
            log.info("Successfully deleted case pointer for caseReference: {}", caseReference);
        } else {
            log.error("No case pointer found to delete for caseReference: {}", caseReference);
        }
    }
}
