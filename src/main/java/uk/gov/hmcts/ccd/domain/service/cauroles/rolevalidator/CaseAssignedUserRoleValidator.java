package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import java.util.List;

public interface CaseAssignedUserRoleValidator {

    boolean canAccessUserCaseRoles(List<String> userIds);
}
