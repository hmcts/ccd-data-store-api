package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);
}
