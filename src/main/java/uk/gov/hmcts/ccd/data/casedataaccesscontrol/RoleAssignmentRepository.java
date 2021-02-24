package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

public interface RoleAssignmentRepository {

    RoleAssignmentResponse getRoleAssignments(String userId);
}
