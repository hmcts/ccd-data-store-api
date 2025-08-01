package uk.gov.hmcts.ccd.domain.service.createevent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronisedCaseViewUpdater {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Executes the provided operation if the case has a version greater than that which we last processed.
     * It uses a pessimistic lock on the case_data row to serialise these operations.
     */
    public void ifFresh(DecentralisedCaseDetails decentralisedCase, Consumer<CaseDetails> operation) {
        var caseDetails = decentralisedCase.getCaseDetails();
        log.debug("Acquiring lock for case reference {}", caseDetails.getReference());
        Integer currentVersion = (Integer) em.createNativeQuery(
                "SELECT version FROM case_data WHERE reference = :ref FOR UPDATE")
            .setParameter("ref", caseDetails.getReference())
            .getSingleResult();
        log.debug("Lock acquired for case reference {}", caseDetails.getReference());

        if (decentralisedCase.getVersion() > currentVersion) {
            log.info("Executing update for case {}, new version {}. Current version is {}.",
                caseDetails.getReference(), caseDetails.getVersion(), currentVersion);

            operation.accept(caseDetails);

            em.createQuery(
                    "UPDATE CaseDetailsEntity SET version = :newVersion WHERE reference = :ref")
                .setParameter("newVersion", decentralisedCase.getVersion().intValue())
                .setParameter("ref", caseDetails.getReference())
                .executeUpdate();
        }
    }
}
