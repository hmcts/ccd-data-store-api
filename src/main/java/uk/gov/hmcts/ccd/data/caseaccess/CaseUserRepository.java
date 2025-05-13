package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;
import java.util.Set;

public interface CaseUserRepository {
    void grantAccess(String caseId, String userId, String caseRole);

    void revokeAccess(String caseId, String userId, String caseRole);

    List<Long> findCasesUserIdHasAccessTo(String userId);

    List<String> findCaseRoles(String caseId, String userId);

    List<CaseUserEntity> findCaseUserRoles(final List<String> caseIds, final List<String> userIds);

    Set<String> getCaseUserRolesByUserId(String userId);
}
