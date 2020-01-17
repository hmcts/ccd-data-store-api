package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Set;

public interface CaseRoleRepository {
    Set<String> getCaseRoles(String caseTypeId);
}
