package uk.gov.hmcts.ccd.data.roleassignment;

public interface RoleAssignmentRepository {

    RoleAssignmentResponse getRoleAssignments(String userId);
}
