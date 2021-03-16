package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

public interface RoleAssignmentRepository {

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse getRoleAssignmentDetails(RoleAssignmentQueryRequest request);

    void deleteRoleAssignment(String assignmentId);

}
