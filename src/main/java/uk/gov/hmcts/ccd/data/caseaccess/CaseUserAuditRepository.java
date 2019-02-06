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

    public void auditGrant(Long caseId, String userId, String caseRole) {
        em.persist(getEntity(caseId, userId, caseRole, GRANT));
    }

    public void auditRevoke(Long caseId, String userId, String caseRole) {
        em.persist(getEntity(caseId, userId, caseRole, REVOKE));
    }

    private CaseUserAuditEntity getEntity(Long caseId, String userId, String caseRole, Action action) {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CaseUserAuditEntity entity = new CaseUserAuditEntity();
        entity.setCaseDataId(caseId);
        entity.setUserId(userId);
        entity.setCaseRole(caseRole);
        entity.setChangedById(principal.getUsername());
        entity.setAction(action);
        return entity;
    }
}
