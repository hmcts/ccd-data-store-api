package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedCaseUserRepository.QUALIFIER)
@RequestScope
public class CachedCaseUserRepository implements CaseUserRepository {

    private final CaseUserRepository caseUserRepository;

    public static final String QUALIFIER = "cached";

    private final Map<String, List<Long>> casesUserHasAccess = newHashMap();
    private final Map<String, List<String>> caseUserRoles = newHashMap();

    public CachedCaseUserRepository(@Qualifier(DefaultCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository) {
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
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        return casesUserHasAccess.computeIfAbsent(userId, e -> caseUserRepository.findCasesUserIdHasAccessTo(userId));
    }

    @Override
    public List<String> findCaseRoles(final Long caseId, final String userId) {
        return caseUserRoles.computeIfAbsent(caseId + userId, e -> caseUserRepository.findCaseRoles(caseId, userId));
    }

    @Override
    public List<CaseUserEntity> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        return caseUserRepository.findCaseUserRoles(caseIds, userIds);
    }
}
