package uk.gov.hmcts.ccd.domain.service.cauroles;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator.CaseAssignedUserRoleValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;
import uk.gov.hmcts.ccd.v2.V2;

@Service
@Qualifier("authorised")
public class AuthorisedCaseAssignedUserRolesOperation implements CaseAssignedUserRolesOperation {

    private final CaseAssignedUserRolesOperation cauRolesOperation;
    private CaseAssignedUserRoleValidator cauRoleValidator;

    @Autowired
    public AuthorisedCaseAssignedUserRolesOperation(final @Qualifier("default") CaseAssignedUserRolesOperation cauRolesOperation,
                                                    @Qualifier("default") final CaseAssignedUserRoleValidator cauRoleValidator) {
        this.cauRolesOperation = cauRolesOperation;
        this.cauRoleValidator = cauRoleValidator;
    }

    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        // NB: Although there are no user based authorisation steps performed here ...
        // ... there are additional s2s authorisation steps performed in the controller.
        this.cauRolesOperation.addCaseUserRoles(caseUserRoles);
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        if (this.cauRoleValidator.canAccessUserCaseRoles(userIds)) {
            return this.cauRolesOperation.findCaseUserRoles(caseIds, userIds);
        }
        throw new CaseRoleAccessException(V2.Error.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED);
    }

}
