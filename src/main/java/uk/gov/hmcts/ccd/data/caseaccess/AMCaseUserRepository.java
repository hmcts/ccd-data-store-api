package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Named
@Singleton
public class AMCaseUserRepository implements CaseUserRepository {

    private final CaseUserAuditRepository auditRepo;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public AMCaseUserRepository(CaseUserAuditRepository caseUserAuditRepository) {
        this.auditRepo = caseUserAuditRepository;
    }

    @Override
    public String getType() {
        return "am";
    }

    @Override
    public void grantAccess(Long caseId, String userId, String caseRole) {

    }

    @Override
    public void revokeAccess(Long caseId, String userId, String caseRole) {

    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        return Lists.newArrayList();
    }

    @Override
    public List<String> findCaseRoles(final Long caseId, final String userId) {
        return Lists.newArrayList();
    }
}
