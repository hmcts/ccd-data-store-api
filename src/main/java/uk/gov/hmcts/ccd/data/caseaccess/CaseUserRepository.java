package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;

public interface CaseUserRepository {

    void grantAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole);

    void revokeAccess(String jurisdictionId, String caseTypeId, String caseReference, Long caseId, String userId, String caseRole);

    List<Long> findCasesUserIdHasAccessTo(String userId);

    List<String> findCaseRoles(String caseTypeId, Long caseId, String userId);
}
