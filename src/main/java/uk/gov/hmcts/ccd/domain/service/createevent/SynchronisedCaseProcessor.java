package uk.gov.hmcts.ccd.domain.service.createevent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.function.Consumer;

/**
 * Provides a concurrency-safe mechanism for processing updates to locally-persisted data for decentralised cases.
 *
 * <p>In the decentralised persistence model, the source of truth for case data lies with decentralised services.
 * However, certain features, such as resolvedTTL and caseLinks, require maintaining derived data in
 * CCD's database.</p>
 *
 * <p>Since decentralised services can process events concurrently for the same case, direct updates to this
 * shared local data would lead to race conditions and potential data corruption. This processor prevents this by
 * serializing updates for a given case reference.</p>
 *
 * <p>It also prevents stale updates by comparing the version number from the decentralised service's response with the
 * last-processed version. An operation is only executed if the incoming version is newer.</p>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronisedCaseProcessor {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Executes the provided operation if the case has a version greater than that which we last processed.
     * It uses a pessimistic lock on the case_data row to serialise these operations.
     * Since we are applying an already-committed change from a decentralised service we use a new transaction
     * independent of any outer transaction, which also minimises the time we hold the rowlock.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyConditionallyWithLock(DecentralisedCaseDetails decentralisedCase,
                                           Consumer<CaseDetails> operation) {
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
