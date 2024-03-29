package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentCategoryService;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Qualifier(DefaultCaseUserRepository.QUALIFIER)
public class DefaultCaseUserRepository implements CaseUserRepository {

    private final CaseUserAuditRepository auditRepo;
    private final RoleAssignmentCategoryService roleAssignmentCategoryService;

    public static final String QUALIFIER = "default";

    @PersistenceContext
    private EntityManager em;

    @Inject
    public DefaultCaseUserRepository(CaseUserAuditRepository caseUserAuditRepository,
                                     RoleAssignmentCategoryService roleAssignmentCategoryService) {
        this.auditRepo = caseUserAuditRepository;
        this.roleAssignmentCategoryService = roleAssignmentCategoryService;
    }

    public void grantAccess(Long caseId, String userId, String caseRole) {
        em.merge(new CaseUserEntity(caseId, userId, caseRole,
            roleAssignmentCategoryService.getRoleCategory(userId).name()));
        auditRepo.auditGrant(caseId, userId, caseRole);
    }

    public void revokeAccess(Long caseId, String userId, String caseRole) {
        CaseUserEntity primaryKey = new CaseUserEntity(caseId, userId, caseRole, null);
        CaseUserEntity caseUser = em.find(CaseUserEntity.class, primaryKey.getCasePrimaryKey());

        if (caseUser != null) {
            em.remove(caseUser);
            auditRepo.auditRevoke(caseId, userId, caseRole);
        }
    }

    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        TypedQuery<Long> namedQuery = em.createNamedQuery(CaseUserEntity.GET_ALL_CASES_USER_HAS_ACCESS_TO, Long.class);
        namedQuery.setParameter("userId", userId);

        return namedQuery.getResultList();
    }

    public List<String> findCaseRoles(final Long caseId, final String userId) {
        TypedQuery<String> namedQuery =
            em.createNamedQuery(CaseUserEntity.GET_ALL_CASE_ROLES_USER_HAS_ACCESS_FOR_A_CASE, String.class);
        namedQuery.setParameter("userId", userId);
        namedQuery.setParameter("caseDataId", caseId);

        return namedQuery.getResultList();
    }

    public List<CaseUserEntity> findCaseUserRoles(final List<Long> caseIds, final List<String> userIds) {
        TypedQuery<CaseUserEntity> namedQuery = null;
        if (userIds.size() == 0) {
            namedQuery = em.createNamedQuery(CaseUserEntity.GET_ALL_CASE_ROLES_BY_CASE_IDS, CaseUserEntity.class);
        } else {
            namedQuery =
                em.createNamedQuery(CaseUserEntity.GET_ALL_CASE_ROLES_USERS_HAS_ACCESS_TO_CASES, CaseUserEntity.class);
            namedQuery.setParameter(CaseUserEntity.PARAM_USER_IDS, userIds);
        }
        namedQuery.setParameter(CaseUserEntity.PARAM_CASE_DATA_IDS, caseIds);
        return namedQuery.getResultList();
    }

    public Set<String> getCaseUserRolesByUserId(String userId) {
        TypedQuery<String> namedQuery = em.createNamedQuery(CaseUserEntity.GET_ALL_CASE_ROLES_BY_USER_ID, String.class);
        namedQuery.setParameter("userId", userId);

        return namedQuery.getResultList().stream().collect(Collectors.toSet());
    }
}
