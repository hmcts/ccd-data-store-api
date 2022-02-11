package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Set;

public interface CaseRoleRepository {
    Set<String> getCaseRoles(String caseTypeId);

    Set<String> getCaseRoles(String userId, String jurisdictionId, String caseTypeId);

    Set<String> getRoles(String caseTypeId);
}
