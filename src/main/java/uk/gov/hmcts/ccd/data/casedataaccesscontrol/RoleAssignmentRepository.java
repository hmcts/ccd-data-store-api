package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    String ROLE_CATEGORY_PROFESSIONAL = "PROFESSIONAL";
    String ROLE_CATEGORY_CITIZEN = "CITIZEN";
    String ROLE_CATEGORY_JUDICIAL = "JUDICIAL";
    String ROLE_CATEGORY_STAFF = "STAFF";

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);
}
