package uk.gov.hmcts.ccd.domain.service.cauroles;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

public interface CaseAssignedUserRolesOperation {

    List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds);

}
