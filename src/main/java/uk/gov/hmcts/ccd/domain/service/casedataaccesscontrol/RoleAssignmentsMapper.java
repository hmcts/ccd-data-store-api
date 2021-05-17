package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

public interface RoleAssignmentsMapper {

    RoleAssignments toRoleAssignments(RoleAssignmentResponse roleAssignmentResponse);
}
