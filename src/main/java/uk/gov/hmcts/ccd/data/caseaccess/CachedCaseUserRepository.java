package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;

@Service
@Singleton
@Qualifier(CachedCaseUserRepository.QUALIFIER)
public class CachedCaseUserRepository implements CaseUserRepository {

    private final CaseUserRepository caseUserRepository;

    public static final String QUALIFIER = "cached";

    public CachedCaseUserRepository(@Qualifier(DefaultCaseUserRepository.QUALIFIER)
                                    final CaseUserRepository caseUserRepository) {
        this.caseUserRepository = caseUserRepository;
    }

    @Override
    public void grantAccess(Long caseId, String userId, String caseRole) {
        caseUserRepository.grantAccess(caseId, userId, caseRole);
    }

    @Override
    public void revokeAccess(Long caseId, String userId, String caseRole) {
        caseUserRepository.revokeAccess(caseId, userId, caseRole);
    }

    @Override
    @Cacheable("casesForUserCache")
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        return caseUserRepository.findCasesUserIdHasAccessTo(userId);
    }

    @Override
    @Cacheable("caseRolesForUserCache")
    public List<String> findCaseRoles(final Long caseId, final String userId) {
        return caseUserRepository.findCaseRoles(caseId, userId);
    }

    @Override
    public List<CaseUserEntity> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        return caseUserRepository.findCaseUserRoles(caseIds, userIds);
    }
}
