package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;

public interface CaseUserRepository {
    void grantAccess(Long caseId, String userId, String caseRole);

    void revokeAccess(Long caseId, String userId, String caseRole);

    List<Long> findCasesUserIdHasAccessTo(String userId);

    List<String> findCaseRoles(Long caseId, String userId);
}
