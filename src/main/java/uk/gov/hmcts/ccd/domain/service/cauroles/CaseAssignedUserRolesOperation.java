package uk.gov.hmcts.ccd.domain.service.cauroles;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleRequest;

public interface CaseAssignedUserRolesOperation {

    void addCaseUserRoles(List<CaseAssignedUserRoleRequest> caseUserRoles);

    List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds);

}
