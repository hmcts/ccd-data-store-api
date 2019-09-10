package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Named
@Singleton
@Qualifier(CCDCaseUserRepository.QUALIFIER)
public class CCDCaseUserRepository implements CaseUserRepository {

    public static final String QUALIFIER = "ccd";
    private final CaseUserAuditRepository auditRepo;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public CCDCaseUserRepository(CaseUserAuditRepository caseUserAuditRepository) {
        this.auditRepo = caseUserAuditRepository;
    }

    @Override
    public void grantAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole) {
        em.merge(new CaseUserEntity(caseId, userId, caseRole));
        auditRepo.auditGrant(caseId, userId, caseRole);
    }

    @Override
    public void revokeAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole) {
        CaseUserEntity primaryKey = new CaseUserEntity(caseId, userId, caseRole);
        CaseUserEntity caseUser = em.find(CaseUserEntity.class, primaryKey.getCasePrimaryKey());

        if (caseUser != null) {
            em.remove(caseUser);
            auditRepo.auditRevoke(caseId, userId, caseRole);
        }
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        TypedQuery<Long> namedQuery = em.createNamedQuery(CaseUserEntity.GET_ALL_CASES_USER_HAS_ACCESS_TO, Long.class);
        namedQuery.setParameter("userId", userId);

        return namedQuery.getResultList();
    }

    @Override
    public List<String> findCaseRoles(final String caseTypeId, final Long caseId, final String userId) {
        TypedQuery<String> namedQuery = em.createNamedQuery(CaseUserEntity.GET_ALL_CASE_ROLES_USER_HAS_ACCESS_FOR_A_CASE, String.class);
        namedQuery.setParameter("userId", userId);
        namedQuery.setParameter("caseDataId", caseId);

        return namedQuery.getResultList();
    }
}
