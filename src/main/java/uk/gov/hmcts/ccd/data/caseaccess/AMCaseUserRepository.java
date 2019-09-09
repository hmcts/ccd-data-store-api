package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Named
@Singleton
@Qualifier(AMCaseUserRepository.QUALIFIER)
public class AMCaseUserRepository implements CaseUserRepository {

    public static final String QUALIFIER = "am";

    @PersistenceContext
    private EntityManager em;

    @Override
    public void grantAccess(String caseTypeId, Long caseId, String userId, String caseRole) {
    }

    @Override
    public void revokeAccess(String caseTypeId, Long caseId, String userId, String caseRole) {
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        return Lists.newArrayList();
    }

    @Override
    public List<String> findCaseRoles(final String casecTypeId, final Long caseId, final String userId) {
        return Lists.newArrayList();
    }
}
