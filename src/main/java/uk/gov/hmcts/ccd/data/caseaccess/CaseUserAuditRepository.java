package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action.GRANT;
import static uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action.REVOKE;

@Named
@Singleton
public class CaseUserAuditRepository {

    @PersistenceContext
    private EntityManager em;

    public void auditGrant(final Long caseId, final String userId) {
        em.persist(getEntity(caseId, userId, GRANT));
    }

    public void auditRevoke(final Long caseId, final String userId) {
        em.persist(getEntity(caseId, userId, REVOKE));
    }

    private CaseUserAuditEntity getEntity(Long caseId, String userId, Action action) {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CaseUserAuditEntity entity = new CaseUserAuditEntity();
        entity.setCaseDataId(caseId);
        entity.setUserId(userId);
        entity.setChangedById(principal.getUsername());
        entity.setAction(action);
        return entity;
    }
}
