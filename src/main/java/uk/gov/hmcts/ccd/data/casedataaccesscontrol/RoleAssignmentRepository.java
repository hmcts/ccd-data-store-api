package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    /* uk.gov.hmcts.reform.roleassignment.domain.model.enums.ActorIdType */
    String ACTOR_ID_TYPE_IDAM = "IDAM";

    /* uk.gov.hmcts.reform.roleassignment.domain.model.enums.Classification */
    String CLASSIFICATION_RESTRICTED = "RESTRICTED";

    /* uk.gov.hmcts.reform.roleassignment.domain.model.enums.RoleCategory */
    String ROLE_CATEGORY_PROFESSIONAL = "PROFESSIONAL";

    /* uk.gov.hmcts.reform.roleassignment.domain.model.enums.RoleCategory */
    String ROLE_TYPE_CASE = "CASE";

    RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest);

    RoleAssignmentResponse getRoleAssignments(String userId);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);

}
