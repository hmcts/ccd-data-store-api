package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    String DEFAULT_PROCESS = "CCD";

    RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest);

    void deleteRoleAssignmentsByQuery(List<RoleAssignmentQuery> queryRequests);

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);

}
