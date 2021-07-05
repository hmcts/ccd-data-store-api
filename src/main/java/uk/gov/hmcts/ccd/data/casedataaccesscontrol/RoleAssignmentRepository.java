package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    String DEFAULT_PROCESS = "CCD";

    RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest);

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);

}
