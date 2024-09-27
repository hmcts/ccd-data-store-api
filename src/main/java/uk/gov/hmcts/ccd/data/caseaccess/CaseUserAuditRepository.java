package uk.gov.hmcts.ccd.data.caseaccess;

import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action.GRANT;
import static uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity.Action.REVOKE;

@Named
@Singleton
public class CaseUserAuditRepository {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private SecurityUtils securityUtils;

    public void auditGrant(Long caseId, String userId, String caseRole) {
        em.persist(getEntity(caseId, userId, caseRole, GRANT));
    }

    public void auditRevoke(Long caseId, String userId, String caseRole) {
        em.persist(getEntity(caseId, userId, caseRole, REVOKE));
    }

    private CaseUserAuditEntity getEntity(Long caseId, String userId, String caseRole, Action action) {
        CaseUserAuditEntity entity = new CaseUserAuditEntity();
        entity.setCaseDataId(caseId);
        entity.setUserId(userId);
        entity.setCaseRole(caseRole);
        entity.setChangedById(securityUtils.getUserId());
        entity.setAction(action);
        return entity;
    }
}
