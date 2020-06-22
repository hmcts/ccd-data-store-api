package uk.gov.hmcts.ccd.data.casedetails;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CasePersistenceException;

@Named
@Qualifier("default")
@Singleton
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSupplementaryDataRepository.class);

    @PersistenceContext
    private EntityManager em;

    private final CaseDetailsMapper caseDetailsMapper;

    @Inject
    public DefaultSupplementaryDataRepository(final CaseDetailsMapper caseDetailsMapper) {
        this.caseDetailsMapper = caseDetailsMapper;
    }


    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        final CaseDetailsEntity newCaseDetailsEntity = caseDetailsMapper.modelToEntity(caseDetails);
        // @Todo enable this once supplementary data last modifed column is added!!
//        newCaseDetailsEntity.setSupplementaryDataLastModified(LocalDateTime.now(ZoneOffset.UTC));
        CaseDetailsEntity mergedEntity;
        try {
            mergedEntity = em.merge(newCaseDetailsEntity);
            em.flush();
        } catch (StaleObjectStateException | OptimisticLockException e) {
            LOG.info("Optimistic Lock Exception: Case data has been altered, UUID={}", caseDetails.getReference(), e);
            throw new CaseConcurrencyException("The case data has been altered outside of this transaction.");
        } catch (PersistenceException e) {
            LOG.warn("Failed to store case details, UUID={}", caseDetails.getReference(), e);
            throw new CasePersistenceException(e.getMessage());
        }
        return caseDetailsMapper.entityToModel(mergedEntity);
    }
}
